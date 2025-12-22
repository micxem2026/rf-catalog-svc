-- Расширение для эффективного ILIKE '%...%' по name
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- LOV_OIP_SUPER_TYPE
CREATE TABLE IF NOT EXISTS LOV_OIP_SUPER_TYPE (
    ID   SERIAL PRIMARY KEY,
    NAME VARCHAR(255) UNIQUE NOT NULL
);

-- LOV_OIP_TYPE
CREATE TABLE IF NOT EXISTS LOV_OIP_TYPE (
    ID   SERIAL PRIMARY KEY,
    ID_OIP_SUPER_TYPE INTEGER NOT NULL REFERENCES LOV_OIP_SUPER_TYPE(ID),
    NAME VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS UNQ_LOV_OIP_TYPE ON LOV_OIP_TYPE(ID_OIP_SUPER_TYPE, NAME);

-- KLF_OIP
CREATE TABLE IF NOT EXISTS KLF_OIP (
    ID                SERIAL PRIMARY KEY,
    GUID              VARCHAR(255) UNIQUE,
    ID_OIP_SUPER_TYPE INTEGER NOT NULL REFERENCES LOV_OIP_SUPER_TYPE(ID),
    ID_OIP_TYPE       INTEGER NOT NULL REFERENCES LOV_OIP_TYPE(ID),
    NAME              VARCHAR(512) NOT NULL,
    PART_NUM          INTEGER NOT NULL DEFAULT 0,
    PART_COUNT        INTEGER NOT NULL DEFAULT 0,
    DURATION          INTERVAL,
    DESCRIPTION       TEXT,
    CREATED_BY        VARCHAR(20)  NOT NULL,
    CREATED_AT        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY        VARCHAR(20),
    UPDATED_AT        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_klf_oip_super_type ON KLF_OIP(ID_OIP_SUPER_TYPE);
CREATE INDEX IF NOT EXISTS idx_klf_oip_type       ON KLF_OIP(ID_OIP_TYPE);
CREATE INDEX IF NOT EXISTS idx_klf_oip_name_trgm  ON KLF_OIP USING gin (NAME gin_trgm_ops);

-- KLF_OIP_HIERARCHY
CREATE TABLE IF NOT EXISTS KLF_OIP_HIERARCHY (
    ID         SERIAL PRIMARY KEY,
    ID_PARENT  INTEGER NOT NULL REFERENCES KLF_OIP(ID),
    ID_OIP     INTEGER NOT NULL REFERENCES KLF_OIP(ID),
    CREATED_BY VARCHAR(20)  NOT NULL,
    CREATED_AT TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(20),
    UPDATED_AT TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_klf_oip_h_parent ON KLF_OIP_HIERARCHY(ID_PARENT);
CREATE INDEX IF NOT EXISTS idx_klf_oip_h_oip    ON KLF_OIP_HIERARCHY(ID_OIP);
CREATE UNIQUE INDEX IF NOT EXISTS UNQ_KLF_OIP_HIERARCHY ON KLF_OIP_HIERARCHY(ID_PARENT, ID_OIP);

DO $$
DECLARE
 v_id_super_type INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM LOV_OIP_SUPER_TYPE WHERE NAME = 'Видео') THEN
       INSERT INTO LOV_OIP_SUPER_TYPE (NAME) VALUES ('Видео')
           RETURNING ID INTO v_id_super_type;

       INSERT INTO LOV_OIP_TYPE (ID_OIP_SUPER_TYPE, NAME) VALUES (v_id_super_type,'Фильм');
       INSERT INTO LOV_OIP_TYPE (ID_OIP_SUPER_TYPE, NAME) VALUES (v_id_super_type,'Сериал');
       INSERT INTO LOV_OIP_TYPE (ID_OIP_SUPER_TYPE, NAME) VALUES (v_id_super_type,'Сезон');
       INSERT INTO LOV_OIP_TYPE (ID_OIP_SUPER_TYPE, NAME) VALUES (v_id_super_type,'Серия');
       INSERT INTO LOV_OIP_TYPE (ID_OIP_SUPER_TYPE, NAME) VALUES (v_id_super_type,'Пакет');
    END IF;
END$$;

-- Добавить колонки
ALTER TABLE KLF_OIP ADD COLUMN IF NOT EXISTS HAS_CHILDREN BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE KLF_OIP ADD COLUMN IF NOT EXISTS HAS_PARENT BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE KLF_OIP ADD COLUMN IF NOT EXISTS CHILDREN_COUNT INTEGER NOT NULL DEFAULT 0;
ALTER TABLE KLF_OIP ADD COLUMN IF NOT EXISTS ROOT_ID INTEGER;
ALTER TABLE KLF_OIP ADD COLUMN IF NOT EXISTS NATIVE_NAME VARCHAR(512);
ALTER TABLE KLF_OIP ADD COLUMN IF NOT EXISTS RELEASE_YEAR VARCHAR(50);


-- partial индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_klf_oip_has_children
    ON KLF_OIP(HAS_CHILDREN) WHERE HAS_CHILDREN = TRUE;

CREATE INDEX IF NOT EXISTS idx_klf_oip_is_leaf
    ON KLF_OIP(HAS_CHILDREN) WHERE HAS_CHILDREN = FALSE;

CREATE INDEX IF NOT EXISTS idx_klf_oip_has_parent
    ON KLF_OIP(HAS_PARENT) WHERE HAS_PARENT = TRUE;

CREATE INDEX IF NOT EXISTS idx_klf_oip_is_root
    ON KLF_OIP(HAS_PARENT) WHERE HAS_PARENT = FALSE;

CREATE INDEX IF NOT EXISTS idx_klf_oip_children_count
    ON KLF_OIP(CHILDREN_COUNT) WHERE CHILDREN_COUNT > 0;

CREATE INDEX IF NOT EXISTS idx_klf_oip_root_id ON KLF_OIP(ROOT_ID);

-- Функция инициализации ROOT_ID для существующих данных
CREATE OR REPLACE FUNCTION initialize_root_ids()
    RETURNS TABLE (
                      updated_count INTEGER,
                      execution_time_ms BIGINT,
                      processed_levels INTEGER,
                      max_depth INTEGER
                  )
    LANGUAGE plpgsql
AS $$
DECLARE
    v_start_time TIMESTAMPTZ;
    v_updated_count INTEGER := 0;
    v_batch_count INTEGER;
    v_current_level INTEGER := 0;
    v_has_more BOOLEAN := TRUE;
BEGIN
    v_start_time := clock_timestamp();

    RAISE NOTICE 'Starting ROOT_ID initialization...';

    -- ШАГ 1: Найти и обновить корневые узлы (которые не имеют родителя)
    UPDATE KLF_OIP o
    SET ROOT_ID = o.ID
    WHERE NOT EXISTS (
        SELECT 1 FROM KLF_OIP_HIERARCHY h WHERE h.ID_OIP = o.ID
    );

    GET DIAGNOSTICS v_batch_count = ROW_COUNT;
    v_updated_count := v_batch_count;

    RAISE NOTICE 'Level 0 (roots): % nodes updated', v_batch_count;

    -- ШАГ 2: Итеративно обрабатываем уровни иерархии сверху вниз
    WHILE v_has_more AND v_current_level < 50 LOOP  -- Защита от бесконечного цикла
        v_current_level := v_current_level + 1;

        -- Обновляем узлы, у которых родитель уже имеет ROOT_ID,
        -- а сам узел еще не обработан
        UPDATE KLF_OIP child
        SET ROOT_ID = parent.ROOT_ID
        FROM KLF_OIP_HIERARCHY h
                 JOIN KLF_OIP parent ON parent.ID = h.ID_PARENT
        WHERE child.ID = h.ID_OIP
          AND child.ROOT_ID IS NULL
          AND parent.ROOT_ID IS NOT NULL;

        GET DIAGNOSTICS v_batch_count = ROW_COUNT;
        v_updated_count := v_updated_count + v_batch_count;

        RAISE NOTICE 'Level %: % nodes updated', v_current_level, v_batch_count;

        -- Если ничего не обновили, значит закончили
        v_has_more := (v_batch_count > 0);
    END LOOP;

    -- ШАГ 3: Проверяем, остались ли необработанные узлы (возможны циклы)
    IF EXISTS (SELECT 1 FROM KLF_OIP WHERE ROOT_ID IS NULL LIMIT 1) THEN
        RAISE WARNING 'Found nodes with NULL ROOT_ID - possible cycles in hierarchy!';

        -- Для оставшихся узлов устанавливаем ROOT_ID = ID (считаем их корнями)
        UPDATE KLF_OIP
        SET ROOT_ID = ID
        WHERE ROOT_ID IS NULL;

        GET DIAGNOSTICS v_batch_count = ROW_COUNT;
        v_updated_count := v_updated_count + v_batch_count;

        RAISE WARNING 'Set ROOT_ID = ID for % orphaned nodes', v_batch_count;
    END IF;

    RETURN QUERY SELECT
                     v_updated_count,
                     EXTRACT(MILLISECONDS FROM (clock_timestamp() - v_start_time))::BIGINT,
                     v_current_level,
                     v_current_level as max_depth;
END;
$$;

COMMENT ON FUNCTION initialize_root_ids() IS 'Оптимизированная инициализация ROOT_ID';

-- Инициализируем ROOT_ID для существующих данных
DO $$
    DECLARE
        v_result RECORD;
    BEGIN
        RAISE NOTICE 'Starting ROOT_ID initialization for existing data...';

        SELECT * INTO v_result FROM initialize_root_ids();

        RAISE NOTICE 'Initialization completed:';
        RAISE NOTICE '  Updated nodes: %', v_result.updated_count;
        RAISE NOTICE '  Execution time: %ms', v_result.execution_time_ms;
        RAISE NOTICE '  Processed levels: %', v_result.processed_levels;
        RAISE NOTICE '  Max depth: %', v_result.max_depth;
    END $$;

-- Создаем NOT NULL constraint после заполнения данных
ALTER TABLE KLF_OIP ALTER COLUMN ROOT_ID SET NOT NULL;

-- Добавляем foreign key для целостности
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_klf_oip_root'
              AND conrelid = 'KLF_OIP'::regclass
        ) THEN
            ALTER TABLE KLF_OIP
                ADD CONSTRAINT fk_klf_oip_root
                    FOREIGN KEY (ROOT_ID)
                        REFERENCES KLF_OIP(ID)
                        ON DELETE RESTRICT;

            RAISE NOTICE 'Foreign key constraint fk_klf_oip_root created';
        ELSE
            RAISE NOTICE 'Foreign key constraint fk_klf_oip_root already exists';
        END IF;
    END $$;


COMMENT ON COLUMN KLF_OIP.ROOT_ID IS
    'Идентификатор корневого ОИС в иерархии. Автоматически вычисляется триггером.';


-- ============================================================================
-- Триггерная функция: Оптимизированное обновление ROOT_ID
-- ============================================================================
CREATE OR REPLACE FUNCTION update_hierarchy_root_ids()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    v_affected_oip_id INTEGER;
BEGIN
    -- Определяем затронутый узел
    IF TG_OP = 'DELETE' THEN
        v_affected_oip_id := OLD.ID_OIP;
    ELSE
        v_affected_oip_id := NEW.ID_OIP;
    END IF;

    -- Одним запросом обновляем все затронутые узлы
    WITH RECURSIVE affected_nodes AS (
        SELECT v_affected_oip_id as node_id

        UNION ALL

        SELECT h.ID_OIP
        FROM affected_nodes an
                 JOIN KLF_OIP_HIERARCHY h ON h.ID_PARENT = an.node_id
    ),
    new_root_ids AS (
       SELECT
           an.node_id,
           COALESCE(
                   (
                       WITH RECURSIVE parent_chain AS (
                           SELECT
                               an.node_id as start_id,
                               an.node_id as current_id,
                               0 as lvl

                           UNION ALL

                           SELECT
                               pc.start_id,
                               h.ID_PARENT,
                               pc.lvl + 1
                           FROM parent_chain pc
                                    JOIN KLF_OIP_HIERARCHY h ON h.ID_OIP = pc.current_id
                           WHERE pc.lvl < 20
                       )
                       SELECT current_id
                       FROM parent_chain
                       WHERE start_id = an.node_id
                       ORDER BY lvl DESC
                       LIMIT 1
                   ),
                   an.node_id
           ) as new_root_id
       FROM affected_nodes an
    )
    UPDATE KLF_OIP o
    SET ROOT_ID = nri.new_root_id
    FROM new_root_ids nri
    WHERE o.ID = nri.node_id
      AND (o.ROOT_ID IS DISTINCT FROM nri.new_root_id);  -- Обновляем только если изменилось

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;


CREATE OR REPLACE FUNCTION insert_hierarchy_root_id()
    RETURNS TRIGGER
AS $$
BEGIN
    NEW.ROOT_ID = NEW.ID;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_hierarchy_root_ids() IS
    'Оптимизированная версия: единым SQL-запросом пересчитывает ROOT_ID
     для всех затронутых узлов при изменении иерархии.';

-- Пересоздаем триггер
DROP TRIGGER IF EXISTS trg_insert_hierarchy_root_id ON KLF_OIP;
DROP TRIGGER IF EXISTS trg_update_hierarchy_root_ids ON KLF_OIP_HIERARCHY;

CREATE TRIGGER trg_insert_hierarchy_root_id
    BEFORE INSERT ON KLF_OIP
    FOR EACH ROW
EXECUTE FUNCTION insert_hierarchy_root_id();

CREATE TRIGGER trg_update_hierarchy_root_ids
    AFTER INSERT OR UPDATE OR DELETE ON KLF_OIP_HIERARCHY
    FOR EACH ROW
EXECUTE FUNCTION update_hierarchy_root_ids();


-- Функция триггера для автоматического обновления флагов
CREATE OR REPLACE FUNCTION update_hierarchy_flags() RETURNS TRIGGER AS $$
DECLARE
    v_parent_count INTEGER;
    v_child_count INTEGER;
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- Увеличить счётчик у родителя
        UPDATE KLF_OIP
        SET HAS_CHILDREN = TRUE,
            CHILDREN_COUNT = CHILDREN_COUNT + 1
        WHERE ID = NEW.ID_PARENT;

        UPDATE KLF_OIP SET HAS_PARENT = TRUE WHERE ID = NEW.ID_OIP;

    ELSIF TG_OP = 'DELETE' THEN
        -- Пересчитать количество потомков у родителя
        SELECT COUNT(*) INTO v_parent_count
        FROM KLF_OIP_HIERARCHY WHERE ID_PARENT = OLD.ID_PARENT;

        UPDATE KLF_OIP
        SET HAS_CHILDREN = (v_parent_count > 0),
            CHILDREN_COUNT = v_parent_count
        WHERE ID = OLD.ID_PARENT;

        -- Проверить наличие родителя у потомка
        SELECT COUNT(*) INTO v_child_count
        FROM KLF_OIP_HIERARCHY WHERE ID_OIP = OLD.ID_OIP;

        UPDATE KLF_OIP
        SET HAS_PARENT = (v_child_count > 0)
        WHERE ID = OLD.ID_OIP;

    ELSIF TG_OP = 'UPDATE' THEN
        -- При изменении связи обновить все 4 записи
        IF OLD.ID_PARENT IS DISTINCT FROM NEW.ID_PARENT OR OLD.ID_OIP IS DISTINCT FROM NEW.ID_OIP THEN
            -- Пересчитать для старого родителя
            SELECT COUNT(*) INTO v_parent_count
            FROM KLF_OIP_HIERARCHY WHERE ID_PARENT = OLD.ID_PARENT;

            UPDATE KLF_OIP
            SET HAS_CHILDREN = (v_parent_count > 0),
                CHILDREN_COUNT = v_parent_count
            WHERE ID = OLD.ID_PARENT;

            -- Увеличить счётчик для нового родителя
            UPDATE KLF_OIP
            SET HAS_CHILDREN = TRUE,
                CHILDREN_COUNT = CHILDREN_COUNT + 1
            WHERE ID = NEW.ID_PARENT;

            -- Обновить флаги для потомков
            SELECT COUNT(*) INTO v_child_count
            FROM KLF_OIP_HIERARCHY WHERE ID_OIP = OLD.ID_OIP;
            UPDATE KLF_OIP SET HAS_PARENT = (v_child_count > 0) WHERE ID = OLD.ID_OIP;

            UPDATE KLF_OIP SET HAS_PARENT = TRUE WHERE ID = NEW.ID_OIP;
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_hierarchy_flags() IS
    'Автоматически обновляет флаги HAS_CHILDREN и HAS_PARENT в таблице KLF_OIP при изменении иерархии';

DROP TRIGGER IF EXISTS trg_update_hierarchy_flags ON KLF_OIP_HIERARCHY;

CREATE TRIGGER trg_update_hierarchy_flags
    AFTER INSERT OR UPDATE OR DELETE ON KLF_OIP_HIERARCHY
    FOR EACH ROW EXECUTE FUNCTION update_hierarchy_flags();

COMMENT ON COLUMN KLF_OIP.HAS_CHILDREN IS
    'Флаг наличия потомков в таблице KLF_OIP_HIERARCHY';

COMMENT ON COLUMN KLF_OIP.HAS_PARENT IS
    'Флаг наличия родителя в таблице KLF_OIP_HIERARCHY';

UPDATE KLF_OIP o
SET CHILDREN_COUNT = (
    SELECT COUNT(*)
    FROM KLF_OIP_HIERARCHY h
    WHERE h.ID_PARENT = o.ID
);

-- Обновить статистику таблицы для оптимизатора
ANALYZE KLF_OIP;
ANALYZE KLF_OIP_HIERARCHY;