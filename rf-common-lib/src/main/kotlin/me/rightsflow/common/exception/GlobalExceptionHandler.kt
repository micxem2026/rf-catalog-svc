package me.rightsflow.common.exception

import io.swagger.v3.oas.annotations.media.Schema
import me.rightsflow.common.config.SecuritySubjectProvider
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler(
    val securitySubjectProvider: SecuritySubjectProvider
) {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(
        ex: EntityNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Entity not found: ${ex.message}")
        return createErrorResponse(
            request = request,
            status = HttpStatus.NOT_FOUND,
            message = "Entity not found with id: ${ex.entityId}"
        )
    }

    @ExceptionHandler(CyclicReferenceException::class)
    fun handleCyclicReferenceException(
        ex: CyclicReferenceException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Cyclic reference detected: ${ex.message}")
        return createErrorResponse(
            request = request,
            status = HttpStatus.CONFLICT,
            message = ex.message ?: "Cyclic reference detected"
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Data integrity violation: ${ex.message}")
        SqlStateExtractor.extractSqlState(ex)?.let { state ->
            if (state.first == "23505") {
                return createErrorResponse(request, HttpStatus.CONFLICT, "${state.second}, SQL State: ${state.first}")
            }
        }
        return createErrorResponse(
            request = request,
            status = HttpStatus.CONFLICT,
            message = "Data integrity violation - entity is being used by other entities"
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Validation error: ${ex.message}")
        val errors = ex.bindingResult.toErrorMessage()

        return createErrorResponse(
            request = request,
            status = HttpStatus.BAD_REQUEST,
            message = "Validation failed: $errors"
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Illegal argument: ${ex.message}")
        return createErrorResponse(
            request = request,
            status = HttpStatus.BAD_REQUEST,
            message = ex.message ?: "Invalid argument"
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Access denied: ${ex.message}")
        return createErrorResponse(
            request = request,
            status = HttpStatus.FORBIDDEN,
            message = HttpStatus.FORBIDDEN.reasonPhrase
        )
    }

    @ExceptionHandler(Throwable::class)
    fun handleOther(ex: Throwable, req: WebRequest): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: ${ex.message}", ex)
        // Спец случай: sqlstate 20101, 20102
        SqlStateExtractor.extractSqlState(ex)?.let { state ->
            if (state.first == "20101" || state.first == "20102") {
                return createErrorResponse(req, HttpStatus.CONFLICT, "${state.second}, SQL State: ${state.first}")
            }
        }
        return createErrorResponse(req, HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "Internal error")
    }


    private fun createErrorResponse(request: WebRequest, status: HttpStatus, message: String) =
        ResponseEntity.status(status).body(
         ErrorResponse(
                    requestUri = request.getDescription(false).removePrefix("uri="),
                    sub = securitySubjectProvider.currentSub(),
                    status = status.value(),
                    message = message,
                    timestamp = LocalDateTime.now()
                )
        )

    fun BindingResult.toErrorMessage(separator: String = "; "): String {
        val messages = mutableListOf<String>()

        // Ошибки конкретных полей
        fieldErrors.forEach { messages.add("${it.field}: ${it.defaultMessage}") }

        // Глобальные ошибки (без поля)
        globalErrors.forEach { messages.add(it.defaultMessage ?: "Validation error") }

        return messages.joinToString(separator)
    }

}

@Schema(description = "Ответ с информацией об ошибке")
data class ErrorResponse(
    @Schema(description = "URI запроса", example = "/api/features/v1/categories/1")
    val requestUri: String,

    @Schema(description = "Субъект (пользователь) из JWT токена", example = "user123")
    val sub: String,

    @Schema(description = "HTTP статус код", example = "400")
    val status: Int,

    @Schema(description = "Сообщение об ошибке", example = "Bad request")
    val message: String,

    @Schema(description = "Время возникновения ошибки")
    val timestamp: LocalDateTime
)
