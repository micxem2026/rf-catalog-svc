package me.rightsflow.acl.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import me.rightsflow.acl.config.TaskStatus
import me.rightsflow.acl.dto.BulkLoadOperationResponse
import me.rightsflow.acl.dto.BulkLoadTaskResponse
import me.rightsflow.acl.dto.TriggersStatusResponse
import me.rightsflow.acl.service.KlfOipHierarchyBulkLoadAsyncService
import me.rightsflow.common.config.CommonSecurityResponses
import me.rightsflow.common.config.InternalServerErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/acl")
@Tag(name = "ACL сервис-методы", description = "Операции для управления длительными процессами загрузки acl-слоя")
class AclBulkLoadController(
    private val asyncService: KlfOipHierarchyBulkLoadAsyncService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/bulk-load-oip-hierarchy/prepare")
    @Operation(summary = "Подготовка таблицы KLF_OIP_HIERARCHY к массовой загрузке данных")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Подготовка таблицы KLF_OIP_HIERARCHY выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun prepareBulkLoadAsync(): ResponseEntity<BulkLoadTaskResponse> {
        val taskId = UUID.randomUUID().toString()
        log.info("Асинхронный запуск подготовки к массовой загрузке, taskId={}", taskId)

        asyncService.prepareBulkLoadAsync(taskId)

        return ResponseEntity.accepted().body(
            BulkLoadTaskResponse(
                taskId = taskId,
                status = TaskStatus.RUNNING,
                message = "Задача подготовки запущена"
            )
        )
    }

    @PostMapping("/bulk-load-oip-hierarchy/finalize")
    @Operation(summary = "Финальная обработка таблицы KLF_OIP_HIERARCHY после массовой загрузки данных")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Финальная обработка таблицы KLF_OIP_HIERARCHY выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun finalizeBulkLoadAsync(): ResponseEntity<BulkLoadTaskResponse> {
        val taskId = UUID.randomUUID().toString()
        log.info("Асинхронный запуск финальной обработки массовой загрузки, taskId={}", taskId)

        asyncService.finalizeBulkLoadAsync(taskId)

        return ResponseEntity.accepted().body(
            BulkLoadTaskResponse(
                taskId = taskId,
                status = TaskStatus.RUNNING,
                message = "Задача финальной обработки запущена"
            )
        )
    }

    @GetMapping("/bulk-load/task/{taskId}")
    @Operation(summary = "Получение статуса асинхронной задачи")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Статус асинхронной задачи получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun getTaskStatus(@PathVariable taskId: String): ResponseEntity<BulkLoadTaskResponse> {
        val task = asyncService.getTaskStatus(taskId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            BulkLoadTaskResponse(
                taskId = task.id,
                status = task.status,
                message = task.message,
                operation = task.operation,
                startTime = task.startTime,
                endTime = task.endTime,
                error = task.error
            )
        )
    }

    /**
     * Аварийное включение триггеров (на случай сбоя)
     */
    @PostMapping("/bulk-load-oip-hierarchy/rollback")
    @Operation(summary = "Аварийное включение триггеров")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Триггеры включены (аварийно)")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun rollbackBulkLoad(): ResponseEntity<BulkLoadOperationResponse> =
            asyncService.rollbackBulkLoad()

    /**
     * Проверка состояния триггеров
     */
    @GetMapping("/bulk-load-oip-hierarchy/trigger-status")
    @Operation(summary = "Проверка статуса триггеров")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Статус триггеров получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun checkTriggersStatus(): ResponseEntity<TriggersStatusResponse> =
            asyncService.checkTriggersStatus()
}