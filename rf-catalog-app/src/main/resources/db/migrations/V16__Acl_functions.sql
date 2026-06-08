-- Удаляем обычный индекс
DROP INDEX IF EXISTS pge_props_composite_idx;
-- Создаем такой же уникальный
CREATE UNIQUE INDEX IF NOT EXISTS pge_props_composite_idx
    ON pge_props USING btree (id_pgl, id_property, id_entity)
    INCLUDE (property_value)
    WITH (fillfactor=80, deduplicate_items=true);

CREATE INDEX IF NOT EXISTS idx_lov_pge_property_code
    ON lov_pge_property (code);

DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'rightsflow'
              AND table_name   = 'pge_props'
              AND column_name  = 'property_value'
              AND data_type   != 'text'
        ) THEN
            ALTER TABLE rightsflow.pge_props
                ALTER COLUMN property_value TYPE text;
        END IF;
    END;
$$;

DROP FUNCTION IF EXISTS pkg_pge.update_property(
    p_code_pg character varying,
    p_property character varying,
    p_id_entity bigint,
    p_value character varying,
    p_username character varying);

CREATE OR REPLACE FUNCTION pkg_pge.update_property(
    p_code_pg character varying,
    p_property character varying,
    p_id_entity bigint,
    p_value character varying,
    p_username character varying DEFAULT 'admin'::character varying)
    RETURNS TABLE(id_entity bigint, id integer, id_pgl integer, id_property integer, pg_order integer, code_prop character varying, name_prop character varying, id_prop_type integer, name_prop_type character varying, property_value text, use_multi_select boolean, display_value text)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    ROWS 1000

    SET search_path=rightsflow
AS $BODY$

#variable_conflict use_column   -- при конфликте имён предпочитать колонку таблицы
declare
    c_Oip     constant integer = 3;
    v_value   varchar;
    v_sel     varchar[];
    v_sql     text;
    v_id_pgl  integer;
    v_id_prop integer;
    v_rec     record;
    v_id_obj  integer;
    v_multi   boolean;
    v_obj     record;
    v_cnt     integer;
    v_id      bigint;
    v_property_format varchar;
begin

    -- находим идентификатор слоя для заданного p_code_pg
    select layer_sel_query into v_sql from lov_pge_property_group where code = p_code_pg;
    execute v_sql into v_sel using p_id_entity;

    select l.id into v_id_pgl from lov_pge_pg_layer l
    join lov_pge_property_group pg on pg.id = l.id_pg
    where l.sel_value::varchar[] && v_sel
      and pg.code = p_code_pg;

    -- находим id_property
    if pkg_pge.is_numeric(p_property) then
        v_id_prop := to_number(p_property, 'FM999999999');
        select p.id into v_id_prop from lov_pge_property p where p.id = v_id_prop;
        if not found then
            raise exception 'Свойство [id=%] не найдено!', v_id_prop
                using errcode = 20150;
        end if;
    else
        select p.id into v_id_prop from lov_pge_property p where p.code = p_property;
        if not found then
            raise exception 'Свойство [code=%] не найдено!', p_property
                using errcode = 20151;
        end if;
    end if;

    select pgld.property_format,
           pt.id_obj,
           pt.use_multi_select
    into v_property_format, v_id_obj, v_multi
    from lov_pge_pgl_dtl pgld
    join lov_pge_property p  on p.id = pgld.id_property
    join lov_pge_prop_type pt on pt.id = p.id_prop_type
    where  pgld.id_pgl = v_id_pgl
      and  pgld.id_property = v_id_prop;

    -- приводим список id к строковому формату postgresql массива
    v_value := p_value;
    if p_value is not null and v_id_obj is not null and v_multi then
        if (p_value ~* '^[0-9]+(,[0-9]+){0,}$') then
            v_value := '{' || p_value || '}';
        end if;
    end if;

    -- проверяем корректность переданного значения свойства
    if v_value is not null and v_property_format is not null then
        if not (v_value ~* v_property_format) then
            raise exception 'Значение [%] не соответствует заданному формату!', v_value
                using errcode = 20152;
        end if;
    end if;

    -- проверяем наличие значения в справочнике для свойств типа "Справочник"
    if v_value is not null and v_id_obj is not null then

        select * into v_obj from lov_object o where o.id = v_id_obj;
        if not found then
            raise exception 'Объект [id=%] не найден!', v_id_obj
                using errcode = 20153;
        end if;

        if v_multi then
            for v_rec in select t.id from unnest(v_value::text[]) as t(id) loop

                    v_sql := 'select count(*) from '||v_obj.table_name||' t where t.id = $1';
                    execute v_sql into v_cnt using to_number(v_rec.id, 'FM999999999');

                    if v_cnt = 0 then
                        raise exception 'Значение [id=%] отсутствует в справочнике "%"!', v_rec.id, v_obj.NAME
                            using errcode = 20154;
                    end if;

                end loop;
        else
            v_sql := 'select count(*) from '||v_obj.table_name||' t where t.id = $1';
            execute v_sql into v_cnt using to_number(v_value, 'FM999999999');

            if v_cnt = 0 then
                raise exception 'Значение [id=%] отсутствует в справочнике "%"!', v_value, v_obj.NAME
                    using errcode = 20154;
            end if;

        end if;

    end if;

    if v_value is not null then
        insert into pge_props (id_pgl, id_property, id_entity, property_value, created_by)
        values (v_id_pgl, v_id_prop, p_id_entity, v_value, p_username)
        on conflict (id_pgl, id_property, id_entity)
            do update set
               property_value = excluded.property_value,
               updated_by     = p_username,
               updated_at     = current_timestamp
        returning id into v_id;

        -- oip-ограничение только для новых записей
        if v_id is not null then
            select pgo.id_obj into v_id_obj
            from lov_pge_pg_to_obj pgo
            where pgo.code_pg = p_code_pg;

            if v_id_obj = c_oip then
                insert into pge_props_oip (id, id_oip)
                values (v_id, p_id_entity)
                on conflict do nothing;
            end if;
        end if;

    else
        -- null-значение — обнуляем запись, если она существует
        update pge_props
        set property_value = null,
            updated_by     = p_username,
            updated_at     = current_timestamp
        where id_pgl      = v_id_pgl
          and id_property = v_id_prop
          and id_entity   = p_id_entity;
    end if;

    return query
        select * from pkg_pge.get_property(p_code_pg, p_property, array[p_id_entity], p_username);
end;
$BODY$;

DROP FUNCTION IF EXISTS pkg_pge.get_property(character varying, character varying, bigint[], character varying);

CREATE OR REPLACE FUNCTION pkg_pge.get_property(
    p_code_pg character varying,
    p_property character varying,
    p_id_entities bigint[],
    p_username character varying DEFAULT 'admin'::character varying)
    RETURNS TABLE(id_entity bigint, id integer, id_pgl integer, id_property integer, pg_order integer, code_prop character varying,
                  name_prop character varying, id_prop_type integer, name_prop_type character varying, property_value text,
                  use_multi_select boolean, display_value text)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    ROWS 1000

    SET search_path=rightsflow
AS $BODY$

begin
    return query
        select t.*
        from pkg_pge.get_pg_data(p_code_pg, p_id_entities, p_username) t
        where case
                  when pkg_pge.is_numeric(p_property)
                      then t.id_property = p_property::integer
                  else t.code_prop = p_property
              end;
end;
$BODY$;

DROP FUNCTION IF EXISTS pkg_pge.get_pg_data(character varying, bigint[], character varying);

CREATE OR REPLACE FUNCTION pkg_pge.get_pg_data(
    p_code_pg character varying,
    p_id_entities bigint[],
    p_username character varying DEFAULT 'admin'::character varying)
    RETURNS TABLE(id_entity bigint, id integer, id_pgl integer, id_property integer, pg_order integer, name_prop character varying,
                  code_prop character varying, id_prop_type integer, name_prop_type character varying, property_value text,
                  use_multi_select boolean, display_value text)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    ROWS 1000

    SET search_path=rightsflow
AS $BODY$

declare
    v_def_id_curr integer;
    v_layer_query text;
    v_rec record;
begin
    -- Валидация входных параметров
    if p_id_entities is null or array_length(p_id_entities, 1) = 0 then
        raise exception 'Массив сущностей пуст'
            using errcode = '20155';
    end if;

    -- Получаем запрос для определения слоев
    select layer_sel_query into strict v_layer_query
    from lov_pge_property_group
    where code = p_code_pg;

    -- Удаляем таблицы если существуют и создаем заново
    drop table if exists temp_entity_layers;

    -- Создаем временную таблицу для слоев сущностей
    create temp table if not exists temp_entity_layers (id_entity bigint, id_pgl integer) on commit drop;

    -- Заполняем слои для каждой сущности
    -- $1 внутри layer_sel_query резолвится в контексте LATERAL как e.id_entity
    execute format(
            'INSERT INTO temp_entity_layers (id_entity, id_pgl)
             SELECT e.id_entity, l.id
             FROM unnest($1) AS e(id_entity)
             CROSS JOIN LATERAL (%s) AS computed(sel_val)
             JOIN lov_pge_pg_layer l
                  ON l.sel_value::varchar[] && computed.sel_val::varchar[]
             JOIN lov_pge_property_group pg ON pg.id = l.id_pg
             WHERE pg.code = %L',
            replace(v_layer_query, '$1', 'e.id_entity'),
            p_code_pg
            ) using p_id_entities;

    -- Создаем индекс для оптимизации
    create index if not exists temp_el_entity_idx on temp_entity_layers(id_entity);
    create index if not exists temp_el_pgl_idx on temp_entity_layers(id_pgl);

    -- Создаем временную таблицу для справочников
    drop table if exists temp_catalogs;
    create temp table if not exists temp_catalogs (id_obj integer, id integer, name text) on commit drop;

    -- Динамически загружаем все необходимые справочники
    for v_rec in
        select distinct
            pt.id_obj,
            lo.table_name,
            lo.where_filter
        from temp_entity_layers el
                 join lov_pge_pgl_dtl pgld on pgld.id_pgl = el.id_pgl
                 join lov_pge_property p on p.id = pgld.id_property
                 join lov_pge_prop_type pt on pt.id = p.id_prop_type
                 join lov_object lo on lo.id = pt.id_obj
        loop
            execute format(
                    'INSERT INTO temp_catalogs (id_obj, id, name)
                     SELECT %L, id, name FROM %s %s',
                    v_rec.id_obj,
                    v_rec.table_name,
                    CASE WHEN v_rec.where_filter IS NOT NULL
                             THEN 'WHERE ' || v_rec.where_filter
                         ELSE '' END
                    );
        end loop;

    -- Создаем индекс для справочников
    create index if not exists temp_cat_obj_id_idx on temp_catalogs(id_obj, id);

    -- Возвращаем результат
    RETURN QUERY
        WITH property_values AS (
            SELECT
                el.id_entity,
                pgld.id,
                pgld.id_pgl,
                pgld.id_property,
                pgld.pg_order,
                p.name            AS name_prop,
                p.code            AS code_prop,
                p.id_prop_type,
                pt.name           AS name_prop_type,
                pt.id_obj,
                pt.use_multi_select,
                CASE
                    WHEN coalesce(pp.property_value, pgld.default_value) = '{DEF_CURRENCY}'
                        THEN v_def_id_curr::varchar
                    ELSE coalesce(pp.property_value, pgld.default_value)
                    END AS property_value
            FROM temp_entity_layers el
                     JOIN lov_pge_pgl_dtl   pgld ON pgld.id_pgl      = el.id_pgl
                     JOIN lov_pge_property  p    ON p.id             = pgld.id_property
                     JOIN lov_pge_prop_type pt   ON pt.id            = p.id_prop_type
                     LEFT JOIN pge_props    pp
                               ON pp.id_pgl      = el.id_pgl
                                   AND pp.id_property = pgld.id_property
                                   AND pp.id_entity   = el.id_entity
        ),
             -- Разворачиваем multi-select значения заранее — один раз для всех строк
             multi_display AS (
                 SELECT
                     pv.id_entity,
                     pv.id_property,
                     string_agg(tc.name, '||' ORDER BY ord.ordinality) AS display_value
                 FROM property_values pv
                          JOIN LATERAL unnest(pv.property_value::integer[])
                     WITH ORDINALITY AS ord(id, ordinality) ON TRUE
                          JOIN temp_catalogs tc
                               ON tc.id     = ord.id
                                   AND tc.id_obj = pv.id_obj
                 WHERE pv.use_multi_select
                   AND pv.id_obj IS NOT NULL
                   AND pv.property_value IS NOT NULL
                 GROUP BY pv.id_entity, pv.id_property
             )
        SELECT
            pv.id_entity,
            pv.id,
            pv.id_pgl,
            pv.id_property,
            pv.pg_order,
            pv.name_prop,
            pv.code_prop,
            pv.id_prop_type,
            pv.name_prop_type,
            pv.property_value,
            pv.use_multi_select,
            CASE
                WHEN pv.use_multi_select AND pv.id_obj IS NOT NULL THEN
                    coalesce(md.display_value, pv.property_value::text)

                WHEN NOT pv.use_multi_select AND pv.id_obj IS NOT NULL THEN
                    coalesce(
                            (SELECT tc.name FROM temp_catalogs tc
                             WHERE tc.id_obj = pv.id_obj
                               AND pv.property_value ~ '^\d+$'
                               AND tc.id = pv.property_value::integer
                             LIMIT 1),
                            pv.property_value::text
                    )

                ELSE pv.property_value::text
                END AS display_value
        FROM property_values pv
                 LEFT JOIN multi_display md
                           ON md.id_entity   = pv.id_entity
                               AND md.id_property = pv.id_property
        ORDER BY pv.id_entity, pv.pg_order;

exception
    when no_data_found then
        raise exception 'Группа свойств [%] не найдена', p_code_pg
            using errcode = '20156';
end;
$BODY$;

CREATE OR REPLACE FUNCTION pkg_pge.update_properties_batch(
    p_updates  text,
    p_username character varying DEFAULT 'system'::character varying)
    RETURNS integer
    LANGUAGE plpgsql
    COST 100
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    SET search_path = rightsflow
AS $BODY$
DECLARE
    c_Oip        CONSTANT integer := 3;
    v_updates    jsonb;
    v_count      integer := 0;
    v_null_count integer := 0;
    v_rec        record;
    v_errors     text;
BEGIN
    -- ── 1. Парсинг и валидация JSON ────────────────────────────────────────
    BEGIN
        v_updates := p_updates::jsonb;
    EXCEPTION
        WHEN invalid_text_representation THEN
            RAISE EXCEPTION 'Некорректный JSON в параметре p_updates: %', p_updates
                USING ERRCODE = '22022';
    END;

    IF jsonb_typeof(v_updates) != 'array' THEN
        RAISE EXCEPTION 'p_updates должен быть JSON массивом'
            USING ERRCODE = '22023';
    END IF;

    -- ── 2. Разворачиваем входной массив во временную таблицу ──────────────
    DROP TABLE IF EXISTS tmp_batch_input;
    CREATE TEMP TABLE tmp_batch_input (
      code_pg   varchar,
      id_entity bigint,
      property  varchar,
      value     varchar
    ) ON COMMIT DROP;

    INSERT INTO tmp_batch_input (code_pg, id_entity, property, value)
    SELECT
        elem->>'code_pg',
        (elem->>'id_entity')::bigint,
        elem->>'property',
        elem->>'value'
    FROM jsonb_array_elements(v_updates) AS elem;

    -- ── 3. Разрешаем property → id_property ───────────────────────────────
    DROP TABLE IF EXISTS tmp_batch_resolved;
    CREATE TEMP TABLE tmp_batch_resolved (
      code_pg     varchar,
      id_entity   bigint,
      property    varchar,   -- исходный идентификатор для сообщений об ошибках
      id_property integer,
      value       varchar
    ) ON COMMIT DROP;

    INSERT INTO tmp_batch_resolved (code_pg, id_entity, property, id_property, value)
    SELECT
        b.code_pg,
        b.id_entity,
        b.property,
        CASE
            WHEN b.property ~ '^\d+$' THEN b.property::integer
            ELSE p.id
            END AS id_property,
        b.value
    FROM tmp_batch_input b
             LEFT JOIN lov_pge_property p
                       ON p.code = b.property
                           AND b.property !~ '^\d+$';

    -- Проверяем что все property разрешились
    SELECT string_agg(
                   format('code_pg=%s, entity=%s, property=%s',
                          br.code_pg, br.id_entity, br.property),
                   '; '
           )
    INTO v_errors
    FROM tmp_batch_resolved br
    WHERE br.id_property IS NULL;

    IF v_errors IS NOT NULL THEN
        RAISE EXCEPTION 'Свойства не найдены в lov_pge_property: %', v_errors
            USING ERRCODE = '20151';
    END IF;

    -- ── 4. Определяем слои для каждой сущности по каждому code_pg ─────────
    --      layer_sel_query содержит $1 (id_entity) — заменяем текстово на
    --      e.id_entity и выполняем через LATERAL: один EXECUTE на code_pg,
    --      а не на каждую сущность
    DROP TABLE IF EXISTS tmp_batch_layers;
    CREATE TEMP TABLE tmp_batch_layers (
      code_pg   varchar,
      id_entity bigint,
      id_pgl    integer
    ) ON COMMIT DROP;

    FOR v_rec IN
        SELECT DISTINCT pg.code, pg.layer_sel_query
        FROM tmp_batch_resolved br
                 JOIN lov_pge_property_group pg ON pg.code = br.code_pg
        LOOP
            EXECUTE format(
                    'INSERT INTO tmp_batch_layers (code_pg, id_entity, id_pgl)
                     SELECT %L, e.id_entity, l.id
                     FROM unnest($1::bigint[]) AS e(id_entity)
                     CROSS JOIN LATERAL (%s) AS computed(sel_val)
                     JOIN lov_pge_pg_layer l
                          ON l.sel_value::varchar[] && computed.sel_val::varchar[]
                     JOIN lov_pge_property_group pg ON pg.id = l.id_pg
                     WHERE pg.code = %L',
                    v_rec.code,
                    replace(v_rec.layer_sel_query, '$1', 'e.id_entity'),
                    v_rec.code
                    ) USING (
                SELECT array_agg(DISTINCT id_entity)
                FROM tmp_batch_resolved
                WHERE code_pg = v_rec.code
            );
        END LOOP;

    CREATE INDEX ON tmp_batch_layers (code_pg, id_entity);
    CREATE INDEX ON tmp_batch_layers (id_pgl);

    -- ── 5. Валидация формата ───────────────────────────────────────────────
    SELECT string_agg(
                   format('property=%s, entity=%s, value=%s, expected=%s',
                          br.property, br.id_entity, br.value, pgld.property_format),
                   '; ' ORDER BY br.property
           )
    INTO v_errors
    FROM tmp_batch_resolved br
             JOIN tmp_batch_layers bl   ON bl.id_entity = br.id_entity AND bl.code_pg = br.code_pg
             JOIN lov_pge_pgl_dtl  pgld ON pgld.id_pgl = bl.id_pgl AND pgld.id_property = br.id_property
    WHERE br.value IS NOT NULL
      AND pgld.property_format IS NOT NULL
      AND NOT (br.value ~* pgld.property_format);

    IF v_errors IS NOT NULL THEN
        RAISE EXCEPTION 'Значения не соответствуют формату: %', v_errors
            USING ERRCODE = '20152';
    END IF;

    -- ── 6. Загружаем справочники для свойств типа "Справочник" ────────────
    DROP TABLE IF EXISTS tmp_batch_catalogs;
    CREATE TEMP TABLE tmp_batch_catalogs (
      id_obj integer,
      id     integer
    ) ON COMMIT DROP;

    FOR v_rec IN
        SELECT DISTINCT pt.id_obj, lo.table_name, lo.where_filter
        FROM tmp_batch_resolved br
                 JOIN tmp_batch_layers bl   ON bl.id_entity = br.id_entity AND bl.code_pg = br.code_pg
                 JOIN lov_pge_pgl_dtl  pgld ON pgld.id_pgl = bl.id_pgl AND pgld.id_property = br.id_property
                 JOIN lov_pge_property p    ON p.id = br.id_property
                 JOIN lov_pge_prop_type pt  ON pt.id = p.id_prop_type
                 JOIN lov_object        lo  ON lo.id = pt.id_obj
        WHERE pt.id_obj IS NOT NULL
          AND br.value IS NOT NULL
        LOOP
            EXECUTE format(
                    'INSERT INTO tmp_batch_catalogs (id_obj, id)
                     SELECT %s, id FROM %s %s',
                    v_rec.id_obj,
                    v_rec.table_name,
                    CASE WHEN v_rec.where_filter IS NOT NULL
                             THEN 'WHERE ' || v_rec.where_filter
                         ELSE '' END
                    );
        END LOOP;

    CREATE INDEX ON tmp_batch_catalogs (id_obj, id);

    -- ── 7а. Валидация single-select ────────────────────────────────────────
    SELECT string_agg(
                   format('property=%s, entity=%s, value=%s',
                          br.property, br.id_entity, br.value),
                   '; '
           )
    INTO v_errors
    FROM tmp_batch_resolved br
             JOIN tmp_batch_layers  bl   ON bl.id_entity = br.id_entity AND bl.code_pg = br.code_pg
             JOIN lov_pge_pgl_dtl   pgld ON pgld.id_pgl = bl.id_pgl AND pgld.id_property = br.id_property
             JOIN lov_pge_property  p    ON p.id = br.id_property
             JOIN lov_pge_prop_type pt   ON pt.id = p.id_prop_type
    WHERE pt.id_obj IS NOT NULL
      AND NOT pt.use_multi_select
      AND br.value IS NOT NULL
      AND br.value ~ '^\d+$'
      AND NOT EXISTS (
        SELECT 1 FROM tmp_batch_catalogs tc
        WHERE tc.id_obj = pt.id_obj
          AND tc.id     = br.value::integer
    );

    IF v_errors IS NOT NULL THEN
        RAISE EXCEPTION 'Значения отсутствуют в справочнике: %', v_errors
            USING ERRCODE = '20154';
    END IF;

    -- ── 7б. Валидация multi-select ─────────────────────────────────────────
    SELECT string_agg(
                   format('property=%s, entity=%s, missing_id=%s',
                          br.property, br.id_entity, val.id),
                   '; '
           )
    INTO v_errors
    FROM tmp_batch_resolved br
             JOIN tmp_batch_layers  bl   ON bl.id_entity = br.id_entity AND bl.code_pg = br.code_pg
             JOIN lov_pge_pgl_dtl   pgld ON pgld.id_pgl = bl.id_pgl AND pgld.id_property = br.id_property
             JOIN lov_pge_property  p    ON p.id = br.id_property
             JOIN lov_pge_prop_type pt   ON pt.id = p.id_prop_type
             CROSS JOIN LATERAL unnest(
            CASE
                WHEN br.value ~* '^[0-9]+(,[0-9]+)*$'
                    THEN ('{' || br.value || '}')::integer[]
                ELSE br.value::integer[]
                END
                                ) AS val(id)
    WHERE pt.id_obj IS NOT NULL
      AND pt.use_multi_select
      AND br.value IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM tmp_batch_catalogs tc
        WHERE tc.id_obj = pt.id_obj
          AND tc.id     = val.id
    );

    IF v_errors IS NOT NULL THEN
        RAISE EXCEPTION 'Элементы мультивыбора отсутствуют в справочнике: %', v_errors
            USING ERRCODE = '20154';
    END IF;

    -- ── 8. UPSERT одной операцией ──────────────────────────────────────────
    WITH upserted AS (
        INSERT INTO pge_props (id_pgl, id_property, id_entity, property_value, created_by)
            SELECT
                bl.id_pgl,
                br.id_property,
                br.id_entity,
                CASE
                    WHEN pt.use_multi_select
                        AND pt.id_obj IS NOT NULL
                        AND br.value ~* '^[0-9]+(,[0-9]+)*$'
                        THEN '{' || br.value || '}'
                    ELSE br.value
                    END AS property_value,
                p_username
            FROM tmp_batch_resolved br
                     JOIN tmp_batch_layers  bl   ON bl.id_entity = br.id_entity AND bl.code_pg = br.code_pg
                     JOIN lov_pge_pgl_dtl   pgld ON pgld.id_pgl = bl.id_pgl AND pgld.id_property = br.id_property
                     JOIN lov_pge_property  p    ON p.id = br.id_property
                     JOIN lov_pge_prop_type pt   ON pt.id = p.id_prop_type
            WHERE br.value IS NOT NULL
            ON CONFLICT (id_pgl, id_property, id_entity)
                DO UPDATE SET
                    property_value = EXCLUDED.property_value,
                    updated_by     = p_username,
                    updated_at     = CURRENT_TIMESTAMP
            RETURNING id, id_entity, id_pgl, (xmax = 0) AS is_insert
    ),
         oip_inserts AS (
             INSERT INTO pge_props_oip (id, id_oip)
                 SELECT u.id, u.id_entity
                 FROM upserted u
                          JOIN tmp_batch_layers  bl  ON bl.id_pgl = u.id_pgl AND bl.id_entity = u.id_entity
                          JOIN lov_pge_pg_to_obj pgo ON pgo.code_pg = bl.code_pg
                 WHERE u.is_insert
                   AND pgo.id_obj = c_Oip
                 ON CONFLICT DO NOTHING
                 RETURNING 1
         )
    SELECT count(*) INTO v_count FROM upserted;

    -- ── 9. Обнуление NULL-значений ─────────────────────────────────────────
    UPDATE pge_props pp
    SET property_value = NULL,
        updated_by     = p_username,
        updated_at     = CURRENT_TIMESTAMP
    FROM tmp_batch_resolved br
             JOIN tmp_batch_layers bl ON bl.id_entity = br.id_entity AND bl.code_pg = br.code_pg
    WHERE pp.id_pgl      = bl.id_pgl
      AND pp.id_property = br.id_property
      AND pp.id_entity   = br.id_entity
      AND br.value IS NULL;

    GET DIAGNOSTICS v_null_count = ROW_COUNT;

    RETURN v_count + v_null_count;
END;
$BODY$;

----

CREATE OR REPLACE FUNCTION pkg_acl.update_age_marker_property(
    p_id_entity bigint,
    p_value character varying,
    p_username character varying DEFAULT 'admin'::character varying)
    RETURNS bigint
    LANGUAGE 'plpgsql'
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    SET search_path=rightsflow
AS $BODY$

declare
    c_Oip     constant integer = 3;
    c_CodePg  constant varchar = 'PG_OIP_VIDEO_PROP';
    c_Property constant varchar = 'ageMarker';
    v_sel     varchar[];
    v_sql     text;
    v_id_pgl  integer;
    v_id_prop integer;
    v_id_obj  integer;
    v_id      bigint;
begin

    -- находим идентификатор слоя для заданного p_code_pg
    select layer_sel_query into v_sql from lov_pge_property_group where code = c_CodePg;
    execute v_sql into v_sel using p_id_entity;

    select l.id into v_id_pgl from lov_pge_pg_layer l
    join lov_pge_property_group pg on pg.id = l.id_pg
    where l.sel_value::varchar[] && v_sel
      and pg.code = c_CodePg;

    -- находим id_property
    select p.id into v_id_prop from lov_pge_property p where p.code = c_Property;
    if not found then
        raise exception 'Свойство [code=%] не найдено!', c_Property
            using errcode = 20151;
    end if;

    if p_value is not null then
        insert into pge_props (id_pgl, id_property, id_entity, property_value, created_by)
        values (v_id_pgl, v_id_prop, p_id_entity, p_value, p_username)
        on conflict (id_pgl, id_property, id_entity)
            do update set
               property_value = excluded.property_value,
               updated_by     = p_username,
               updated_at     = current_timestamp
        returning id into v_id;

        -- oip-ограничение только для новых записей
        if v_id is not null then
            select pgo.id_obj into v_id_obj
            from lov_pge_pg_to_obj pgo
            where pgo.code_pg = c_CodePg;

            if v_id_obj = c_oip then
                insert into pge_props_oip (id, id_oip)
                values (v_id, p_id_entity)
                on conflict do nothing;
            end if;
        end if;

    else
        -- null-значение — обнуляем запись, если она существует
        update pge_props
        set property_value = null,
            updated_by     = p_username,
            updated_at     = current_timestamp
        where id_pgl      = v_id_pgl
          and id_property = v_id_prop
          and id_entity   = p_id_entity;
    end if;

    return coalesce(v_id, -1);
end;
$BODY$;

DROP FUNCTION IF EXISTS pkg_acl.sync_klf_oip(integer, character varying, integer, integer, character varying, character varying,
                                             character varying, character varying, integer, integer, character varying, text,
                                             boolean, boolean, integer, integer, boolean);

CREATE OR REPLACE FUNCTION pkg_acl.sync_klf_oip(
    p_id integer,
    p_guid character varying,
    p_id_oip_super_type integer,
    p_id_oip_type integer,
    p_name character varying,
    p_native_name character varying,
    p_full_name character varying,
    p_release_year character varying,
    p_part_num integer,
    p_part_count integer,
    p_duration character varying,
    p_description text,
    p_id_age_marker integer,
    p_has_children boolean,
    p_has_parent boolean,
    p_children_count integer,
    p_root_id integer,
    p_drop_flag boolean)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    SET search_path=rightsflow
AS $BODY$

declare
    v_id integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_oip where id = p_id;
    else
        insert into klf_oip (id, guid, id_oip_super_type, id_oip_type, name, native_name, full_name, release_year, part_num,
                             part_count, duration, description, has_children, has_parent, children_count, root_id, created_by )
        values (p_id, p_guid, p_id_oip_super_type, p_id_oip_type, p_name, p_native_name,
                p_full_name, p_release_year, p_part_num, p_part_count, nullif(p_duration,'')::interval,
                p_description, p_has_children, p_has_parent, p_children_count,
                p_root_id, 'system')
        on conflict (id) do update set
           guid = excluded.guid,
           id_oip_super_type = excluded.id_oip_super_type,
           id_oip_type = excluded.id_oip_type,
           name = excluded.name,
           native_name = excluded.native_name,
           full_name = excluded.full_name,
           release_year = excluded.release_year,
           part_num = excluded.part_num,
           part_count = excluded.part_count,
           duration = excluded.duration,
           description = excluded.description,
           has_children = excluded.has_children,
           has_parent = excluded.has_parent,
           children_count = excluded.children_count,
           root_id = excluded.root_id,
           updated_by = 'system',
           updated_at = current_timestamp
        returning id into v_id;

        if p_id_age_marker is not null then
            perform pkg_acl.update_age_marker_property(
                    p_id_entity => p_id,
                    p_value => p_id_age_marker::text,
                    p_username => 'system'
                    );
        end if;

    end if;
    return v_id;
end;
$BODY$;

ALTER FUNCTION pkg_acl.sync_klf_oip(integer, character varying, integer, integer, character varying, character varying,
                                    character varying, character varying, integer, integer, character varying, text,
                                    integer, boolean, boolean, integer, integer, boolean)
    OWNER TO rightsflow;

CREATE OR REPLACE FUNCTION pkg_acl.sync_klf_oip_properties(p_props_json text)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE SECURITY DEFINER PARALLEL UNSAFE
    SET search_path=rightsflow
AS $BODY$

begin
    perform pkg_pge.update_properties_batch(
               p_updates => p_props_json,
               p_username => 'system'
            );
    return -1;
end;
$BODY$;
