package me.rightsflow.acl.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import me.rightsflow.acl.dto.InitHierarchyResultDto
import me.rightsflow.acl.dto.TriggersStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class KlfOipHierarchyBulkLoadService(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(KlfOipHierarchyBulkLoadService::class.java)

    /**
     * Отключение триггеров и подготовка таблиц
     */
    fun disableTriggersAndPrepare() {
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.timeout = 300 // 5 минут

        transactionTemplate.execute { _ ->
            log.info("Отключение триггеров на таблице klf_oip_hierarchy...")

            // Отключаем триггеры
            entityManager.createNativeQuery(
                """
                ALTER TABLE klf_oip_hierarchy 
                    DISABLE TRIGGER trg_update_hierarchy_flags
                """.trimIndent()
            ).executeUpdate()

            entityManager.createNativeQuery(
                """
                ALTER TABLE klf_oip_hierarchy 
                    DISABLE TRIGGER trg_update_hierarchy_root_ids
                """.trimIndent()
            ).executeUpdate()

            entityManager.createNativeQuery(
                """
                ALTER TABLE klf_oip 
                    DISABLE TRIGGER trg_insert_hierarchy_root_id
                """.trimIndent()
            ).executeUpdate()

            log.info("Триггеры отключены")

            entityManager.createNativeQuery(
                """
                ALTER TABLE IF EXISTS klf_oip DROP CONSTRAINT IF EXISTS fk_klf_oip_root
                """.trimIndent()
            ).executeUpdate()

            log.info("Ограничение RootId отключено")

            // Подготавливаем klf_oip
/*            log.info("Подготовка таблицы klf_oip...")

            entityManager.createNativeQuery(
                """
                ALTER TABLE IF EXISTS klf_oip 
                    ALTER COLUMN root_id DROP NOT NULL
                """.trimIndent()
            ).executeUpdate()

            entityManager.createNativeQuery(
                "UPDATE klf_oip SET root_id = NULL"
            ).executeUpdate()

            log.info("Таблица klf_oip подготовлена")*/

        }
    }

    /**
     * Финализация: включение триггеров
     */
    fun finalizeAndEnableTriggers() {
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.timeout = 300 // на пересчет (для больших объемов)

        transactionTemplate.execute { _ ->
            try {
                log.info("Начало финальной обработки массовой загрузки ОИС...")

                // Шаг 1: Анализируем таблицы
                log.info("Выполнение ANALYZE...")
                entityManager.createNativeQuery("ANALYZE klf_oip").executeUpdate()
                entityManager.createNativeQuery("ANALYZE klf_oip_hierarchy").executeUpdate()
                log.info("ANALYZE выполнен")

                // Шаг 2: Включаем триггеры
                enableTriggersOnly()

                try {
                    entityManager.createNativeQuery(
                        """
                        ALTER TABLE rightsflow.klf_oip ADD CONSTRAINT fk_klf_oip_root 
                        FOREIGN KEY (root_id) REFERENCES rightsflow.klf_oip (id)
                        """.trimIndent()
                    ).executeUpdate()
                    log.info("Ограничение RootId включено")
                } catch (e: Exception) {
                    log.warn("Ограничение RootId включить не удалось: ${e.message}")
                }

                log.info("=== Финальная обработка массовой загрузки ОИС завершена успешно ===")

            } catch (e: Exception) {
                log.error("Ошибка при финальной обработки, попытка включить триггеры...", e)
                try {
                    enableTriggersOnly()
                } catch (rollbackError: Exception) {
                    log.error("Критическая ошибка: не удалось включить триггеры!", rollbackError)
                }
                throw e
            }
        }
    }

    /**
     * Включение только триггеров (для восстановления после ошибки)
     */
    fun enableTriggersOnly() {
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute { _ ->
            log.info("Включение триггеров...")

            entityManager.createNativeQuery(
                """
                ALTER TABLE klf_oip 
                    ENABLE TRIGGER trg_insert_hierarchy_root_id
                """.trimIndent()
            ).executeUpdate()

            entityManager.createNativeQuery(
                """
                ALTER TABLE klf_oip_hierarchy 
                    ENABLE TRIGGER trg_update_hierarchy_flags
                """.trimIndent()
            ).executeUpdate()

            entityManager.createNativeQuery(
                """
                ALTER TABLE klf_oip_hierarchy 
                    ENABLE TRIGGER trg_update_hierarchy_root_ids
                """.trimIndent()
            ).executeUpdate()

            log.info("Триггеры включены")
        }
    }

    /**
     * Проверка статуса триггеров
     */
    fun checkTriggersStatus(): TriggersStatusResponse {
        val transactionTemplate = TransactionTemplate(transactionManager)

        return transactionTemplate.execute { _ ->
            // Проверяем статус триггеров
            val query = entityManager.createNativeQuery(
                """
                SELECT 
                    t.tgname AS trigger_name,
                    t.tgenabled <> 'D' AS is_enabled
                FROM pg_trigger t
                JOIN pg_class c ON c.oid = t.tgrelid
                WHERE c.relname = 'klf_oip_hierarchy'
                  AND t.tgname IN ('trg_update_hierarchy_flags', 'trg_update_hierarchy_root_ids')
                  AND NOT t.tgisinternal
                """.trimIndent()
            )

            @Suppress("UNCHECKED_CAST")
            val results = query.resultList as List<Array<Any>>
            val triggerStatuses = results.associate {
                it[0].toString() to (it[1].toString().toBoolean())
            }

            // Проверяем nullable root_id
            val nullableQuery = entityManager.createNativeQuery(
                """
                SELECT 
                  conname 
                FROM pg_constraint 
                WHERE conname = 'fk_klf_oip_root' 
                  AND conrelid = 'rightsflow.klf_oip'::regclass;
                """.trimIndent()
            )
            val hasRootConstraint = nullableQuery.singleResult.toString() == "fk_klf_oip_root"

            TriggersStatusResponse(
                triggersEnabled = triggerStatuses.values.all { it },
                triggerStatuses = triggerStatuses,
                hasRootIdConstraint = hasRootConstraint
            )
        } ?: TriggersStatusResponse(false, emptyMap(), true)
    }
}