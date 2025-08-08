CREATE OR REPLACE FUNCTION ins_klf_feature_tree(
    p_id_parent INTEGER,
    p_id_feature_plain INTEGER,
    p_created_by VARCHAR(20),
    p_beg_date DATE default null,
    p_end_date DATE default null
)
    RETURNS INTEGER AS  $$
DECLARE
    v_validity_period daterange;
    v_id_feature_category INTEGER;
    v_cnt INTEGER;
    r_result INTEGER;
BEGIN

    v_validity_period := daterange(p_beg_date, p_end_date, '[]');

    select id_feature_category into v_id_feature_category from klf_feature_plain
    where id = p_id_feature_plain limit 1;

    if v_id_feature_category is null then
        raise exception 'Характеристика не найдена [ID_FEATURE_PLAIN=%]', p_id_feature_plain
            using errcode = 20100;
    end if;

    -- Проверка: если p_id_parent IS NULL, то цикла быть не может
    if p_id_parent is null then
        select id into r_result from klf_feature_tree
        where id_parent is null and id_feature_plain = p_id_feature_plain;
        if r_result is null then
            insert into klf_feature_tree (
                id_parent, id_feature_category, id_feature_plain, validity_period, created_by
            ) values (
                         p_id_parent, v_id_feature_category, p_id_feature_plain, v_validity_period, p_created_by
                     ) returning id into r_result;
        end if;
        return r_result;
    end if;

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

    -- Если проверка пройдена — вставляем

    select id into r_result from klf_feature_tree
    where id_parent = p_id_parent and id_feature_plain = p_id_feature_plain;
    if r_result is null then
        insert into klf_feature_tree (
            id_parent, id_feature_category, id_feature_plain, validity_period, created_by
        ) values (
                     p_id_parent, v_id_feature_category, p_id_feature_plain, v_validity_period, p_created_by
                 ) returning id into r_result;
    end if;
    return r_result;

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION upd_klf_feature_tree(
    p_id INTEGER,
    p_id_parent INTEGER,
    p_id_feature_plain INTEGER,
    p_updated_by VARCHAR(20),
    p_beg_date DATE default null,
    p_end_date DATE default null
)
    RETURNS INTEGER AS  $$
DECLARE
    v_cnt INTEGER;
    v_old klf_feature_tree%ROWTYPE;
    v_new klf_feature_tree%ROWTYPE;
BEGIN

    -- читаем старое значение записи
    select * into v_old from klf_feature_tree
    where id = p_id;

    select p_id_parent,
           case when p_id_feature_plain is null then v_old.id_feature_plain else p_id_feature_plain end,
           p_updated_by,
           case when coalesce(p_beg_date, '1900-01-01'::date) <= coalesce(p_end_date, '3000-01-01'::date)
                    then daterange(p_beg_date, p_end_date, '[]') else v_old.validity_period end
    into v_new.id_parent, v_new.id_feature_plain, v_new.updated_by, v_new.validity_period;

    select id_feature_category into v_new.id_feature_category from klf_feature_plain
    where id = v_new.id_feature_plain limit 1;

    if v_new.id_feature_category is null then
        raise exception 'Характеристика не найдена [ID_FEATURE_PLAIN=%]', v_new.id_feature_plain
            using errcode = 20100;
    end if;

    -- Проверка: если v_new.id_parent IS NULL, то цикла быть не может
    if v_new.id_parent is null then
        select count(*) into v_cnt from klf_feature_tree
        where id_parent is null and id_feature_plain = v_new.id_feature_plain and id <> p_id;
        if v_cnt = 0 then
            update klf_feature_tree
            set id_parent = null,
                id_feature_category = v_new.id_feature_category,
                id_feature_plain = v_new.id_feature_plain,
                validity_period = v_new.validity_period,
                updated_by = v_new.updated_by,
                updated_at = CURRENT_TIMESTAMP
            where id = p_id;
        else
            raise exception 'Ошибка обновления! Конфликт с существующим элементом [ID_PARENT=null, ID_FEATURE_PLAIN=%]!', v_new.id_feature_plain
                using errcode = 20102;
        end if;
        return p_id;
    end if;

    -- Проверка: нельзя добавить элемент, который встречается в цепочке родителей текущего родителя элемента
    with recursive feature_tree(id, id_parent, level) as (
        select id_feature_plain, id_parent, 1 as level from klf_feature_tree
        where id = v_new.id_parent
        union
        select r.id_feature_plain, r.id_parent, s.level+1 as level from klf_feature_tree r, feature_tree s
        where s.id_parent = r.id
    )
    select count(*) into v_cnt from feature_tree where id = v_new.id_feature_plain;

    -- Если узел с таким id_feature_plain уже есть в цепочке родителей — это цикл
    if v_cnt > 0 then
        raise exception 'Цикл в дереве характеристик: узел с id_feature_plain = % уже находится в цепочке родителей', v_new.id_feature_plain
            using errcode = 20101;
    end if;

    -- Если проверка пройдена — вставляем

    select count(*) into v_cnt from klf_feature_tree
    where id_parent = v_new.id_parent and id_feature_plain = v_new.id_feature_plain and id <> p_id;
    if v_cnt = 0 then
        update klf_feature_tree
        set id_parent = v_new.id_parent,
            id_feature_category = v_new.id_feature_category,
            id_feature_plain = v_new.id_feature_plain,
            validity_period = v_new.validity_period,
            updated_by = v_new.updated_by,
            updated_at = CURRENT_TIMESTAMP
        where id = p_id;
    else
        raise exception 'Ошибка обновления! Конфликт с существующим элементом [ID_PARENT=%, ID_FEATURE_PLAIN=%]!', v_new.id_parent, v_new.id_feature_plain
            using errcode = 20102;
    end if;
    return p_id;

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION rightsflow.sync_users(
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
    p_user_type character varying DEFAULT 'USER'::character varying)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
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
$BODY$;
