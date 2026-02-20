package me.rightsflow.acl.service

import me.rightsflow.acl.config.TaskStatus
import me.rightsflow.acl.dto.BulkLoadOperationResponse
import me.rightsflow.acl.dto.BulkLoadTask
import me.rightsflow.acl.dto.TriggersStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class KlfOipHierarchyBulkLoadAsyncService(
    private val bulkLoadService: KlfOipHierarchyBulkLoadService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Хранилище задач
    private val tasks = ConcurrentHashMap<String, BulkLoadTask>()

    @Async("bulkLoadTaskExecutor")
    fun prepareBulkLoadAsync(taskId: String) {
        val task = BulkLoadTask(
            id = taskId,
            operation = "PREPARE",
            status = TaskStatus.RUNNING,
            startTime = OffsetDateTime.now()
        )
        tasks[taskId] = task

        try {
            log.info("Выполнение подготовки, taskId={}", taskId)
            bulkLoadService.disableTriggersAndPrepare()

            tasks[taskId] = task.copy(
                status = TaskStatus.COMPLETED,
                endTime = OffsetDateTime.now(),
                message = "Триггеры отключены, таблицы подготовлены"
            )
            log.info("Подготовка завершена успешно, taskId={}", taskId)

        } catch (e: Exception) {
            log.error("Ошибка подготовки, taskId={}", taskId, e)
            tasks[taskId] = task.copy(
                status = TaskStatus.FAILED,
                endTime = OffsetDateTime.now(),
                message = "Ошибка: ${e.message}",
                error = e.stackTraceToString()
            )
        }
    }

    @Async("bulkLoadTaskExecutor")
    fun finalizeBulkLoadAsync(taskId: String) {
        val task = BulkLoadTask(
            id = taskId,
            operation = "FINALIZE",
            status = TaskStatus.RUNNING,
            startTime = OffsetDateTime.now()
        )
        tasks[taskId] = task

        try {
            log.info("Выполнение финализации, taskId={}", taskId)
            bulkLoadService.finalizeAndEnableTriggers()

            tasks[taskId] = task.copy(
                status = TaskStatus.COMPLETED,
                endTime = OffsetDateTime.now(),
                message = "Данные пересчитаны, триггеры включены"
            )
            log.info("Финализация завершена успешно, taskId={}", taskId)

        } catch (e: Exception) {
            log.error("Ошибка финализации, taskId={}", taskId, e)
            tasks[taskId] = task.copy(
                status = TaskStatus.FAILED,
                endTime = OffsetDateTime.now(),
                message = "Ошибка: ${e.message}",
                error = e.stackTraceToString()
            )
        }
    }

    fun rollbackBulkLoad(): ResponseEntity<BulkLoadOperationResponse> {
        return try {
            log.warn("Запрос на аварийное включение триггеров")
            bulkLoadService.enableTriggersOnly()

            ResponseEntity.ok(
                BulkLoadOperationResponse(
                    success = true,
                    message = "Триггеры включены (аварийное восстановление)",
                    timestamp = OffsetDateTime.now()
                )
            )
        } catch (e: Exception) {
            log.error("Ошибка аварийного восстановления", e)
            ResponseEntity.status(500).body(
                BulkLoadOperationResponse(
                    success = false,
                    message = "Ошибка: ${e.message}",
                    timestamp = OffsetDateTime.now()
                )
            )
        }
    }

    fun checkTriggersStatus(): ResponseEntity<TriggersStatusResponse> {
        return try {
            val status = bulkLoadService.checkTriggersStatus()
            ResponseEntity.ok(status)
        } catch (e: Exception) {
            log.error("Ошибка проверки статуса триггеров", e)
            ResponseEntity.status(500).build()
        }
    }

    fun getTaskStatus(taskId: String): BulkLoadTask? {
        return tasks[taskId]
    }
}