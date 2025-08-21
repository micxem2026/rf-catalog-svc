CREATE OR REPLACE FUNCTION rightsflow.ins_klf_oip_hierarchy(
    p_id_parent integer,
    p_id_oip integer,
    p_created_by character varying)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    v_cnt INTEGER;
    r_result INTEGER;
BEGIN

    -- Проверка: если p_id_parent IS NULL, то выдаём ошибку
    if p_id_parent is null then
        raise exception 'Родитель не может быть NULL для ОИС [id_oip = %]', p_id_oip
            using errcode = 20103;
    end if;

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
    select id into r_result from klf_oip_hierarchy
    where id_parent = p_id_parent and id_oip = p_id_oip;
    if r_result is null then
        insert into klf_oip_hierarchy (id_parent, id_oip, created_by)
        values (p_id_parent, p_id_oip, p_created_by)
        returning id into r_result;
    end if;
    return r_result;

END;
$BODY$;