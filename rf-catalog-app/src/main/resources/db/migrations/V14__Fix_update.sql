create or replace function pkg_pge.get_pg_data(
    p_code_pg character varying,
    p_id_entities bigint[],
    p_username character varying DEFAULT 'admin'::character varying
)
    returns table(
                     id_entity bigint,
                     id integer,
                     id_pgl integer,
                     id_property integer,
                     pg_order integer,
                     name_prop character varying,
                     code_prop character varying,
                     id_prop_type integer,
                     name_prop_type character varying,
                     property_value character varying,
                     use_multi_select boolean,
                     display_value text
                 )
    security definer
    SET search_path = rightsflow
    language plpgsql
as
$$
declare
    v_def_id_curr integer;
    v_layer_query text;
    v_catalog_sql text;
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
    drop table if exists temp_catalogs;

    -- Создаем временную таблицу для слоев сущностей
    create temp table if not exists temp_entity_layers (id_entity bigint, id_pgl integer) on commit drop;

    -- Заполняем слои для каждой сущности
    for v_rec in
        select unnest(p_id_entities) as id_entity
        loop
            execute format(
                    'insert into temp_entity_layers (id_entity, id_pgl)
                     select $1, l.id
                     from lov_pge_pg_layer l
                     join lov_pge_property_group pg on pg.id = l.id_pg
                     where l.sel_value::varchar[] && (%s)::varchar[]
                     and pg.code = $2', v_layer_query
                    ) using v_rec.id_entity, p_code_pg;
        end loop;

    -- Создаем индекс для оптимизации
    create index if not exists temp_el_entity_idx on temp_entity_layers(id_entity);
    create index if not exists temp_el_pgl_idx on temp_entity_layers(id_pgl);

    -- Создаем временную таблицу для справочников
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
            v_catalog_sql := format(
                    'insert into temp_catalogs (id_obj, id, name)
                     select %s, id, name from %s %s',
                    v_rec.id_obj,
                    v_rec.table_name,
                    case when v_rec.where_filter is not null
                             then 'where ' || v_rec.where_filter
                         else ''
                        end
                             );
            execute v_catalog_sql;
        end loop;

    -- Создаем индекс для справочников
    create index if not exists temp_cat_obj_id_idx on temp_catalogs(id_obj, id);

    -- Возвращаем результат
    return query
        with property_values as (
            select
                el.id_entity,
                pgld.id,
                pgld.id_pgl,
                pgld.id_property,
                pgld.pg_order,
                p.name as name_prop,
                p.code as code_prop,
                p.id_prop_type,
                pt.name as name_prop_type,
                pt.id_obj,
                pt.use_multi_select,
                case
                    when coalesce(pp.property_value, pgld.default_value) = '{DEF_CURRENCY}'
                        then v_def_id_curr::varchar
                    else coalesce(pp.property_value, pgld.default_value)
                    end as property_value
            from temp_entity_layers el
                     join lov_pge_pgl_dtl pgld on pgld.id_pgl = el.id_pgl
                     join lov_pge_property p on p.id = pgld.id_property
                     join lov_pge_prop_type pt on pt.id = p.id_prop_type
                     left join pge_props pp
                               on pp.id_pgl = el.id_pgl
                                   and pp.id_property = pgld.id_property
                                   and pp.id_entity = el.id_entity
        )
        select
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
            case
                when pv.use_multi_select and pv.id_obj is not null then
                    coalesce(
                            (select string_agg(tc.name, '||' order by ordinality)
                             from unnest(pv.property_value::integer[]) with ordinality as val(id, ordinality)
                                      join temp_catalogs tc
                                           on tc.id = val.id
                                               and tc.id_obj = pv.id_obj
                            )::text,
                            pv.property_value::text
                    )
                when not pv.use_multi_select and pv.id_obj is not null then
                    coalesce(
                            (select tc.name
                             from temp_catalogs tc
                             where tc.id_obj = pv.id_obj
                               and pv.property_value ~ '^\d+$'
                               and tc.id = pv.property_value::integer
                            )::text,
                            pv.property_value::text
                    )
                else
                    pv.property_value::text
                end as display_value
        from property_values pv
        order by pv.id_entity, pv.pg_order;

exception
    when no_data_found then
        raise exception 'Группа свойств [%] или валюта по умолчанию не найдена', p_code_pg
            using errcode = '20156';
end;
$$;