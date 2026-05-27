package me.rightsflow.common.exception

import io.swagger.v3.oas.annotations.media.Schema
import me.rightsflow.common.config.SecuritySubjectProvider
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
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

    @ExceptionHandler(EntityNotFoundException::class, EntityNotFoundWithClsException::class)
    fun handleEntityNotFoundException(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(ex.message ?: "Entity not found")
        return createErrorResponse(
            request = request,
            status = HttpStatus.NOT_FOUND,
            message = ex.message ?: "Entity not found"
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

    @ExceptionHandler(ConstraintException::class)
    fun handleConstraintException(
        ex: ConstraintException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Constraint violation detected: ${ex.message}")
        return createErrorResponse(
            request = request,
            status = HttpStatus.CONFLICT,
            message = ex.message ?: "Constraint violation detected"
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Data integrity violation: ${ex.message}")
        SqlStateExtractor.extractSqlState(ex)?.let { state ->
            if (state.first == "23505" || state.first == "23503") {
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
            message = ex.message ?: HttpStatus.FORBIDDEN.reasonPhrase
        )
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleSpringSecurityAccessDeniedException(
        ex: org.springframework.security.access.AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Access denied: ${ex.message}")
        return createErrorResponse(
            request = request,
            status = HttpStatus.FORBIDDEN,
            message = ex.message ?: HttpStatus.FORBIDDEN.reasonPhrase
        )
    }

    @ExceptionHandler(Throwable::class)
    fun handleOther(ex: Throwable, req: WebRequest): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: ${ex.message}", ex)

        SqlStateExtractor.extractSqlState(ex)?.let { state ->
            val sqlState = state.first
            val message  = state.second

            // SQLSTATE 42501 — insufficient_privilege (PostgreSQL стандарт)
            // Бросается из pkg_contract.check_contract_org_access при отказе в доступе
            if (sqlState == "42501") {
                log.warn("Org access denied for user '{}': {}", securitySubjectProvider.currentSub(), message)
                return createErrorResponse(req, HttpStatus.FORBIDDEN, message)
            }

            val intState = sqlState.toIntOrNull()
            if (((intState ?: 0) in 20100..20200) || (intState ?: 0) == 23505 || (intState ?: 0) == 23503) {
                return createErrorResponse(req, HttpStatus.CONFLICT, "$message, SQL State: $sqlState")
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
    @field:Schema(description = "URI запроса", example = "/api/features/v1/categories/1")
    val requestUri: String,

    @field:Schema(description = "Субъект (пользователь) из JWT токена", example = "user123")
    val sub: String,

    @field:Schema(description = "HTTP статус код", example = "400")
    val status: Int,

    @field:Schema(description = "Сообщение об ошибке", example = "Bad request")
    val message: String,

    @field:Schema(description = "Время возникновения ошибки")
    val timestamp: LocalDateTime
)
