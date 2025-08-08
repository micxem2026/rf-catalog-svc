package me.rightsflow.features.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(
        ex: EntityNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Entity not found: ${ex.message}")
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.NOT_FOUND,
            message = "Entity not found with id: ${ex.entityId}"
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(CyclicReferenceException::class)
    fun handleCyclicReferenceException(
        ex: CyclicReferenceException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Cyclic reference detected: ${ex.message}")
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.CONFLICT,
            message = ex.message ?: "Cyclic reference detected"
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Data integrity violation: ${ex.message}")
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.CONFLICT,
            message = "Data integrity violation - entity is being used by other entities"
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Validation error: ${ex.message}")
        val errors = ex.bindingResult.allErrors.joinToString("; ") { error ->
            when (error) {
                is FieldError -> "${error.field}: ${error.defaultMessage}"
                else -> error.defaultMessage ?: "Validation error"
            }
        }
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.BAD_REQUEST,
            message = "Validation failed: $errors"
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Illegal argument: ${ex.message}")
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.BAD_REQUEST,
            message = ex.message ?: "Invalid argument"
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Access denied: ${ex.message}")
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.FORBIDDEN,
            message = HttpStatus.FORBIDDEN.reasonPhrase
        )
        return ResponseEntity(errorResponse, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: ${ex.message}", ex)
        val errorResponse = createErrorResponse(
            request = request,
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun createErrorResponse(
        request: WebRequest,
        status: HttpStatus,
        message: String
    ): ErrorResponse {
        val authentication = SecurityContextHolder.getContext().authentication
        val subject = when (authentication) {
            is JwtAuthenticationToken -> authentication.token.subject
            else -> "anonymous"
        }

        return ErrorResponse(
            requestUri = request.getDescription(false).removePrefix("uri="),
            sub = subject,
            status = status.value(),
            message = message,
            timestamp = LocalDateTime.now()
        )
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
