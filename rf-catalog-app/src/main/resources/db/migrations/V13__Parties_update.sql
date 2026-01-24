ALTER TABLE KLF_COUNTERPARTY ADD COLUMN IF NOT EXISTS CODE_1C VARCHAR(50) UNIQUE;
ALTER TABLE KLF_ORGANIZATION ADD COLUMN IF NOT EXISTS CODE_1C VARCHAR(50) UNIQUE;

-- LOV_RIGHT_GROUP
CREATE TABLE IF NOT EXISTS LOV_RIGHT_GROUP (
  ID   SERIAL PRIMARY KEY,
  NAME VARCHAR(255) UNIQUE NOT NULL
);
COMMENT ON TABLE LOV_RIGHT_GROUP IS 'Список групп прав';

INSERT INTO LOV_RIGHT_GROUP (ID,NAME) VALUES
  (1,'Эфирное право'),
  (2,'Цифровое право'),
  (3,'Мерчандайз право'),
  (4,'Другое право')
ON CONFLICT (ID) DO NOTHING;

ALTER TABLE KLF_RIGHT_TYPE ADD COLUMN IF NOT EXISTS ID_RIGHT_GROUP INTEGER DEFAULT 4 REFERENCES LOV_RIGHT_GROUP(ID);

UPDATE KLF_RIGHT_TYPE SET ID_RIGHT_GROUP = 4, UPDATED_BY = 'admin', UPDATED_AT = CURRENT_TIMESTAMP;

ALTER TABLE KLF_RIGHT_TYPE ALTER COLUMN ID_RIGHT_GROUP SET NOT NULL;

drop function if exists rightsflow.ins_klf_right_type(
    p_id_parent integer,
    p_name character varying,
    p_description character varying,
    p_created_by character varying
);

drop function if exists rightsflow.upd_klf_right_type(
    p_id integer,
    p_id_parent integer,
    p_name character varying,
    p_description character varying,
    p_updated_by character varying
);

CREATE OR REPLACE FUNCTION rightsflow.ins_klf_right_type(
    p_id_parent integer,
    p_name character varying,
    p_description character varying,
    p_id_right_group integer,
    p_created_by character varying)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
DECLARE
    r_result integer;
BEGIN

    if p_id_parent is null then
        select id into r_result from klf_right_type
        where id_parent is null and lower(name) = lower(p_name);
        if r_result is null then
            insert into klf_right_type(id_parent, name, description, id_right_group, created_by)
            values (p_id_parent, p_name, p_description, p_id_right_group,p_created_by)
            returning id into r_result;
        end if;
        return r_result;
    end if;

    select id into r_result from klf_right_type
    where id_parent = p_id_parent and lower(name) = lower(p_name);
    if r_result is null then
        insert into klf_right_type (id_parent, name, description, id_right_group, created_by)
        values (p_id_parent, p_name, p_description, p_id_right_group,p_created_by)
        returning id into r_result;
    end if;
    return r_result;

END;
$BODY$;

CREATE OR REPLACE FUNCTION rightsflow.upd_klf_right_type(
    p_id integer,
    p_id_parent integer,
    p_name character varying,
    p_description character varying,
    p_id_right_group integer,
    p_updated_by character varying)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
DECLARE
    v_cnt INTEGER;
    v_old klf_right_type%ROWTYPE;
    v_new klf_right_type%ROWTYPE;
BEGIN

    select * into v_old from klf_right_type
    where id = p_id;

    select p_id_parent, case when p_name is null then v_old.name else p_name end, p_updated_by, p_description, p_id_right_group
    into v_new.id_parent, v_new.name, v_new.updated_by, v_new.description, v_new.id_right_group;

    if v_new.id_parent is null then
        select count(*) into v_cnt from klf_right_type
        where id_parent is null and lower(name) = lower(v_new.name) and id <> p_id;
        if v_cnt = 0 then
            update klf_right_type
            set id_parent = null,
                name = v_new.name,
                description = v_new.description,
                id_right_group = v_new.id_right_group,
                updated_by = v_new.updated_by,
                updated_at = CURRENT_TIMESTAMP
            where id = p_id;
        else
            raise exception 'Ошибка обновления! Конфликт с существующим элементом [ID_PARENT=null, NAME=%]!', v_new.name
                using errcode = 20102;
        end if;
        return p_id;
    end if;

    select count(*) into v_cnt from klf_right_type
    where id_parent = v_new.id_parent and lower(name) = lower(v_new.name) and id <> p_id;
    if v_cnt = 0 then
        update klf_right_type
        set id_parent = v_new.id_parent,
            name = v_new.name,
            description = v_new.description,
            id_right_group = v_new.id_right_group,
            updated_by = v_new.updated_by,
            updated_at = CURRENT_TIMESTAMP
        where id = p_id;
    else
        raise exception 'Ошибка обновления! Конфликт с существующим элементом [ID_PARENT=%, NAME=%]!', v_new.id_parent, v_new.name
            using errcode = 20102;
    end if;
    return p_id;

END;
$BODY$;