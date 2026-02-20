create schema if not exists pkg_acl;
alter schema pkg_acl owner to rightsflow;

create or replace function pkg_acl.sync_klf_right_type(
    p_id integer,
    p_id_parent integer,
    p_name character varying,
    p_description character varying,
    p_id_right_group integer,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_right_type where id = p_id;
    else
        insert into klf_right_type (id, id_parent, name, description, id_right_group, created_by)
        values (p_id, p_id_parent, p_name, p_description, p_id_right_group, 'system')
        on conflict (id) do update set
           id_parent = excluded.id_parent,
           name = excluded.name,
           description = excluded.description,
           id_right_group = excluded.id_right_group,
           updated_by = 'system',
           updated_at = current_timestamp
        returning id into v_id;
    end if;
    return v_id;
end;
$$;

create or replace function pkg_acl.sync_klf_feature_plain(
    p_id integer,
    p_id_feature_category integer,
    p_name character varying,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_feature_plain where id = p_id;
    else
        insert into klf_feature_plain (id, id_feature_category, name, created_by)
        values (p_id, p_id_feature_category, p_name, 'system')
        on conflict (id) do update set
           id_feature_category = excluded.id_feature_category,
           name = excluded.name,
           updated_by = 'system',
           updated_at = current_timestamp
        returning id into v_id;
    end if;
    return v_id;
end;
$$;

create or replace function pkg_acl.sync_klf_feature_tree(
    p_id integer,
    p_id_parent integer,
    p_id_feature_category integer,
    p_id_feature_plain integer,
    p_beg_date date,
    p_end_date date,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
    v_cnt integer;
    v_validity_period daterange;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_feature_tree where id = p_id;
    else
        v_validity_period := daterange(p_beg_date, p_end_date, '[]');

        -- Проверка: нельзя добавить элемент, который встречается в цепочке родителей текущего родителя элемента
        with recursive feature_tree(id, id_parent, level) as (
            select id_feature_plain, id_parent, 1 as level from klf_feature_tree
            where id = p_id_parent
            union
            select r.id_feature_plain, r.id_parent, s.level+1 as level from klf_feature_tree r, feature_tree s
            where s.id_parent = r.id
        )
        select count(*) into v_cnt from feature_tree where id = p_id_feature_plain;

        -- Если узел с таким id_feature_plain уже есть в цепочке родителей — это цикл
        if v_cnt > 0 then
            raise exception 'Цикл в дереве характеристик: узел с id_feature_plain = % уже находится в цепочке родителей', p_id_feature_plain
                using errcode = 20101;
        end if;

        select count(*) into v_cnt from klf_feature_tree where id = p_id;
        if v_cnt = 0 then
            insert into klf_feature_tree (id, id_parent, id_feature_category, id_feature_plain, validity_period, created_by)
            values (p_id, p_id_parent, p_id_feature_category, p_id_feature_plain,
                    v_validity_period, 'system')
            returning  id into v_id;
        else
            select count(*) into v_cnt from klf_feature_tree
            where id_parent = p_id_parent and id_feature_plain = p_id_feature_plain and id <> p_id;
            if v_cnt = 0 then
                update klf_feature_tree set
                    id_parent = p_id_parent,
                    id_feature_category = p_id_feature_category,
                    id_feature_plain = p_id_feature_plain,
                    validity_period = v_validity_period,
                    updated_by = 'system',
                    updated_at = current_timestamp
                where id = p_id
                returning id into v_id;
            else
                raise exception 'Ошибка обновления! Конфликт с существующим элементом [ID_PARENT=%, ID_FEATURE_PLAIN=%]!',
                    p_id_parent, p_id_feature_plain
                    using errcode = 20102;
            end if;
        end if;
    end if;
    return v_id;
end;
$$;

create or replace function pkg_acl.sync_lov_oip_type(
    p_id integer,
    p_id_oip_super_type integer,
    p_name character varying,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from lov_oip_type where id = p_id;
    else
        insert into lov_oip_type (id, id_oip_super_type, name)
        values (p_id, p_id_oip_super_type, p_name)
        on conflict (id) do update set
           id_oip_super_type = excluded.id_oip_super_type,
           name = excluded.name
        returning id into v_id;
    end if;
    return v_id;
end;
$$;

create or replace function pkg_acl.sync_klf_oip(
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
    p_has_children boolean,
    p_has_parent boolean,
    p_children_count integer,
    p_root_id integer,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
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
    end if;
    return v_id;
end;
$$;

create or replace function pkg_acl.sync_klf_oip_hierarchy(
    p_id integer,
    p_id_parent integer,
    p_id_oip integer,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
    v_cnt integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_oip_hierarchy where id = p_id;
    else
        -- Проверка: если p_id_parent IS NULL, то выдаём ошибку
        if p_id_parent is null then
            raise exception 'Родитель не может быть NULL для ОИС [id_oip = %]', p_id_oip
                using errcode = 20103;
        end if;

        if not exists(select 1 from klf_oip_hierarchy where id = p_id) then
            -- Проверка: нельзя добавить элемент, который встречается в цепочке родителей текущего родителя элемента
            with recursive oip_tree(id_parent, id_oip, level) as (
                select id_parent, id_oip, 1 as level from klf_oip_hierarchy
                where id_oip = p_id_parent
                union
                select r.id_parent, r.id_oip, s.level+1 as level from klf_oip_hierarchy r, oip_tree s
                where s.id_parent = r.id_oip and s.level < 20
            )
            select count(*) into v_cnt from oip_tree
            where id_parent = p_id_oip or id_oip = p_id_oip;

            -- Если узел с таким id_oip уже есть в цепочке родителей — это цикл
            if v_cnt > 0  or p_id_oip = p_id_parent then
                raise exception 'Цикл в дереве ОИС: узел с id_oip = % уже находится в цепочке родителей', p_id_oip
                    using errcode = 20101;
            end if;

            -- Если проверка пройдена — вставляем
            select id into v_id from klf_oip_hierarchy
              where id_parent = p_id_parent
                and id_oip = p_id_oip;
            if v_id is null then
                insert into klf_oip_hierarchy (id, id_parent, id_oip, created_by)
                values (p_id, p_id_parent, p_id_oip, 'system')
                returning id into v_id;
            end if;
        else
            v_id := p_id;
        end if;
    end if;
    return v_id;
end;
$$;

CREATE OR REPLACE FUNCTION rightsflow.insert_hierarchy_root_id()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$

BEGIN
    if NEW.ROOT_ID is NULL THEN
       NEW.ROOT_ID = NEW.ID;
    END IF;
    RETURN NEW;
END;
$BODY$;

ALTER TABLE KLF_COUNTERPARTY ADD COLUMN IF NOT EXISTS COUNTRY VARCHAR(32);
ALTER TABLE KLF_COUNTERPARTY ADD COLUMN IF NOT EXISTS ADDRESS TEXT;
ALTER TABLE KLF_COUNTERPARTY ADD COLUMN IF NOT EXISTS TIN VARCHAR(32);

ALTER TABLE KLF_ORGANIZATION ADD COLUMN IF NOT EXISTS COUNTRY VARCHAR(32);
ALTER TABLE KLF_ORGANIZATION ADD COLUMN IF NOT EXISTS ADDRESS TEXT;
ALTER TABLE KLF_ORGANIZATION ADD COLUMN IF NOT EXISTS TIN VARCHAR(32);

create or replace function pkg_acl.sync_klf_counterparty(
    p_id integer,
    p_name character varying,
    p_code_1c character varying,
    p_country character varying,
    p_address text,
    p_tin character varying,
    p_id_org_ref integer,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_counterparty where id = p_id;
    else
        insert into klf_counterparty (id, name, code_1c, country, address, tin, id_org_ref, created_by)
        values (p_id, p_name, p_code_1c, p_country, p_address, p_tin,
                p_id_org_ref, 'system')
        on conflict (id) do update set
           name = excluded.name,
           code_1c = excluded.code_1c,
           country = excluded.country,
           address = excluded.address,
           tin = excluded.tin,
           id_org_ref = excluded.id_org_ref,
           updated_by = 'system',
           updated_at = current_timestamp
        returning id into v_id;
    end if;
    return v_id;
end;
$$;

create or replace function pkg_acl.sync_klf_organization(
    p_id integer,
    p_name character varying,
    p_code_1c character varying,
    p_country character varying,
    p_address text,
    p_tin character varying,
    p_drop_flag boolean
) returns integer
    security definer
    set search_path = rightsflow
    language plpgsql
    volatile parallel unsafe
as
$$
declare
    v_id integer;
begin
    if p_drop_flag then
        v_id := -1;
        delete from klf_organization where id = p_id;
    else
        insert into klf_organization (id, name, code_1c, country, address, tin, created_by)
        values (p_id, p_name, p_code_1c, p_country, p_address, p_tin, 'system')
        on conflict (id) do update set
           name = excluded.name,
           code_1c = excluded.code_1c,
           country = excluded.country,
           address = excluded.address,
           tin = excluded.tin,
           updated_by = 'system',
           updated_at = current_timestamp
        returning id into v_id;
    end if;
    return v_id;
end;
$$;

--==============================================================
create schema if not exists pkg_sync;
alter schema pkg_sync owner to rightsflow;

CREATE UNLOGGED TABLE IF NOT EXISTS SYNC__LOV_SOFTWARE_SYSTEM
(
    ID INTEGER NOT NULL PRIMARY KEY,
    NAME VARCHAR(255) UNIQUE NOT NULL
);
COMMENT ON TABLE SYNC__LOV_SOFTWARE_SYSTEM IS 'Список программных систем';

CREATE UNLOGGED TABLE IF NOT EXISTS SYNC__LOV_SOFTWARE_OBJECT
(
    ID INTEGER NOT NULL PRIMARY KEY,
    NAME VARCHAR(255) UNIQUE NOT NULL
);
COMMENT ON TABLE SYNC__LOV_SOFTWARE_OBJECT IS 'Список сущностей/объектов системы';

CREATE TABLE IF NOT EXISTS SYNC__KEY_MAPPING
(
    ID BIGSERIAL NOT NULL PRIMARY KEY,
    ID_SW_SYS INTEGER NOT NULL,
    ID_SW_OBJ INTEGER NOT NULL,
    ID_RF  BIGINT,
    ID_EXT BIGINT,
    CREATED_BY  VARCHAR(20)  NOT NULL,
    CREATED_AT  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY  VARCHAR(20),
    UPDATED_AT  TIMESTAMPTZ
);
COMMENT ON TABLE SYNC__KEY_MAPPING IS 'Таблица связи ключей rightsflow с ключами внешних систем';

-- Составной индекс для быстрого поиска значений ключей
CREATE INDEX IF NOT EXISTS idx_key_mapping_ext_lookup
    ON SYNC__KEY_MAPPING (id_sw_sys, id_sw_obj, id_ext)
    INCLUDE (id_rf);

CREATE INDEX IF NOT EXISTS idx_key_mapping_rf_lookup
    ON SYNC__KEY_MAPPING (id_sw_sys, id_sw_obj, id_rf)
    INCLUDE (id_ext);

CREATE OR REPLACE FUNCTION pkg_acl.get_rf_key(
    p_id_sw_sys INTEGER,
    p_id_sw_obj INTEGER,
    p_id_ext    BIGINT
)
    RETURNS BIGINT
    SECURITY DEFINER
    SET search_path = rightsflow
    LANGUAGE plpgsql
    STABLE PARALLEL SAFE
AS $$
DECLARE
    v_rf_id BIGINT;
BEGIN
    SELECT id_rf
    INTO v_rf_id
    FROM SYNC__KEY_MAPPING
    WHERE id_sw_sys = p_id_sw_sys
      AND id_sw_obj = p_id_sw_obj
      AND id_ext = p_id_ext
    LIMIT 1;  -- Защита от дубликатов (хотя уникальность должна быть обеспечена бизнес-логикой)

    RETURN v_rf_id;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Ошибка в get_rf_key(%, %, %): %',
            p_id_sw_sys, p_id_sw_obj, p_id_ext, SQLERRM;
        RETURN NULL;
END;
$$;

COMMENT ON FUNCTION pkg_acl.get_rf_key(INTEGER, INTEGER, BIGINT)
    IS 'Преобразование внешнего ключа → ключ RightsFlow. Использует индекс idx_key_mapping_ext_lookup.';

CREATE OR REPLACE FUNCTION pkg_acl.get_ext_key(
    p_id_sw_sys INTEGER,
    p_id_sw_obj INTEGER,
    p_id_rf     BIGINT
)
    RETURNS BIGINT
    SECURITY DEFINER
    SET search_path = rightsflow
    LANGUAGE plpgsql
    STABLE PARALLEL SAFE
AS $$
DECLARE
    v_ext_id BIGINT;
BEGIN
    SELECT id_ext
    INTO v_ext_id
    FROM SYNC__KEY_MAPPING
    WHERE id_sw_sys = p_id_sw_sys
      AND id_sw_obj = p_id_sw_obj
      AND id_rf = p_id_rf
    LIMIT 1;

    RETURN v_ext_id;
EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Ошибка в get_ext_key(%, %, %): %',
            p_id_sw_sys, p_id_sw_obj, p_id_rf, SQLERRM;
        RETURN NULL;
END;
$$;

COMMENT ON FUNCTION pkg_acl.get_ext_key(INTEGER, INTEGER, BIGINT)
    IS 'Преобразование ключа RightsFlow → внешний ключ. Использует индекс idx_key_mapping_rf_lookup.';

DROP FUNCTION IF EXISTS rightsflow.sync_users(int4, int4, varchar, varchar, varchar, varchar, bool, bool, bool, timestamp, timestamp, timestamp, timestamp, varchar);

CREATE OR REPLACE FUNCTION pkg_sync.sync_users(
   p_sync_id integer,
   p_id integer,
   p_username character varying,
   p_display_name character varying,
   p_email character varying,
   p_password_hash character varying,
   p_enabled boolean DEFAULT true,
   p_account_non_expired boolean DEFAULT false,
   p_account_non_locked boolean DEFAULT true,
   p_expiration_date timestamp without time zone DEFAULT NULL::timestamp without time zone,
   p_last_logon timestamp without time zone DEFAULT NULL::timestamp without time zone,
   p_created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
   p_updated_at timestamp without time zone DEFAULT NULL::timestamp without time zone,
   p_user_type character varying DEFAULT 'USER'::character varying
) RETURNS integer
  SECURITY DEFINER
  SET search_path = rightsflow
  LANGUAGE plpgsql
AS $function$
BEGIN
    -- Обработка удаления записи
    if p_id is null then
        DELETE FROM sync__users WHERE id = p_sync_id;
        -- Обработка добавления и обновления записи
    else
        INSERT INTO sync__users (
            id, username, display_name, email, password_hash,
            enabled, account_non_expired, account_non_locked,
            expiration_date, last_logon, created_at, updated_at,
            user_type
        )
        VALUES (
                   p_sync_id, p_username, p_display_name, p_email, p_password_hash,
                   p_enabled, p_account_non_expired, p_account_non_locked,
                   p_expiration_date, p_last_logon, p_created_at, p_updated_at,
                   p_user_type
               )
        ON CONFLICT (id) DO UPDATE SET
           display_name = EXCLUDED.display_name,
           email = EXCLUDED.email,
           password_hash = EXCLUDED.password_hash,
           enabled = EXCLUDED.enabled,
           account_non_expired = EXCLUDED.account_non_expired,
           account_non_locked = EXCLUDED.account_non_locked,
           expiration_date = EXCLUDED.expiration_date,
           last_logon = EXCLUDED.last_logon,
           updated_at = EXCLUDED.updated_at,
           user_type = EXCLUDED.user_type
        WHERE
            EXCLUDED.updated_at > COALESCE(sync__users.updated_at, '1970-01-01'::TIMESTAMP);
    end if;

    RETURN p_sync_id;
END;
$function$;

CREATE OR REPLACE FUNCTION pkg_sync.sync_lov_software_system(
   p_sync_id integer,
   p_id integer,
   p_name character varying
) RETURNS integer
  SECURITY DEFINER
  SET search_path = rightsflow
  LANGUAGE plpgsql
AS $function$
BEGIN
    -- Обработка удаления записи
    if p_id is null then
        DELETE FROM sync__lov_software_system WHERE id = p_sync_id;
        -- Обработка добавления и обновления записи
    else
        INSERT INTO sync__lov_software_system (
            id, name
        )
        VALUES (
                 p_sync_id, p_name
               )
        ON CONFLICT (id) DO UPDATE SET
           name = EXCLUDED.name;
    end if;

    RETURN p_sync_id;
END;
$function$;

CREATE OR REPLACE FUNCTION pkg_sync.sync_lov_software_object(
    p_sync_id integer,
    p_id integer,
    p_name character varying
) RETURNS integer
    SECURITY DEFINER
    SET search_path = rightsflow
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Обработка удаления записи
    if p_id is null then
        DELETE FROM sync__lov_software_object WHERE id = p_sync_id;
        -- Обработка добавления и обновления записи
    else
        INSERT INTO sync__lov_software_object (
            id, name
        )
        VALUES (
                   p_sync_id, p_name
               )
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name;
    end if;

    RETURN p_sync_id;
END;
$function$;

CREATE OR REPLACE FUNCTION pkg_sync.sync_key_mapping(
    p_sync_id bigint,
    p_id bigint,
    p_id_sw_sys integer,
    p_id_sw_obj integer,
    p_id_rf bigint,
    p_id_ext bigint,
    p_created_by character varying,
    p_created_at timestamp with time zone,
    p_updated_by character varying,
    p_updated_at timestamp with time zone
) RETURNS integer
    SECURITY DEFINER
    SET search_path = rightsflow
    LANGUAGE plpgsql
AS $function$
BEGIN
    -- Обработка удаления записи
    if p_id is null then
        DELETE FROM sync__key_mapping WHERE id = p_sync_id;
        -- Обработка добавления и обновления записи
    else
        INSERT INTO sync__key_mapping (
            id, id_sw_sys, id_sw_obj, id_rf, id_ext, created_by, created_at, updated_by, updated_at
        )
        VALUES (
                 p_sync_id, p_id_sw_sys, p_id_sw_obj, p_id_rf, p_id_ext,
                 p_created_by, p_created_at, p_updated_by, p_updated_at
               )
        ON CONFLICT (id) DO UPDATE SET
            id_sw_sys = excluded.id_sw_sys,
            id_sw_obj = excluded.id_sw_obj,
            id_rf = excluded.id_rf,
            id_ext = excluded.id_ext,
            created_by = excluded.created_by,
            created_at = excluded.created_at,
            updated_by = excluded.updated_by,
            updated_at = excluded.updated_at
        WHERE
            excluded.updated_at > COALESCE(sync__key_mapping.updated_at, '1970-01-01'::TIMESTAMP);
    end if;

    RETURN p_sync_id;
END;
$function$;

INSERT INTO KAFKA_BINDINGS_CONTROL (BINDING_NAME, BINDING_STATE)
VALUES
    ('swSystemProcessor-in-0','PAUSE')
ON CONFLICT (BINDING_NAME) DO NOTHING;

INSERT INTO KAFKA_BINDINGS_CONTROL (BINDING_NAME, BINDING_STATE)
VALUES
    ('swObjectProcessor-in-0','PAUSE')
ON CONFLICT (BINDING_NAME) DO NOTHING;

INSERT INTO KAFKA_BINDINGS_CONTROL (BINDING_NAME, BINDING_STATE)
VALUES
    ('keyMappingProcessor-in-0','PAUSE')
ON CONFLICT (BINDING_NAME) DO NOTHING;

--===================================================