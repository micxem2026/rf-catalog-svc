package me.rightsflow.features.config

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import me.rightsflow.features.exception.ErrorResponse


// Общие ошибки аутентификации и авторизации
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "401",
            description = "Не авторизован",
            content = [Content(

            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав",
            content = [Content(

            )]
        )
    ]
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommonSecurityResponses

// Ошибка "не найдено"
@ApiResponse(
    responseCode = "404",
    description = "Сущность не найдена",
    content = [Content(

    )]
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotFoundResponse

// Ошибка валидации
@ApiResponse(
    responseCode = "400",
    description = "Некорректные данные",
    content = [Content(
        mediaType = "application/json",
        schema = Schema(implementation = ErrorResponse::class)
    )]
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidationErrorResponse

// Ошибка конфликта
@ApiResponse(
    responseCode = "409",
    description = "Конфликт данных",
    content = [Content(

    )]
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConflictResponse

// Внутренняя ошибка сервера
@ApiResponse(
    responseCode = "500",
    description = "Внутренняя ошибка сервера",
    content = [Content(

    )]
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class InternalServerErrorResponse
