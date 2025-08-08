package me.rightsflow.features.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание простой характеристики")
data class CreateFeaturePlainRequest(
    @field:NotBlank(message = "Название характеристики не может быть пустым")
    @field:Size(max = 255, message = "Название характеристики не может превышать 255 символов")
    @Schema(description = "Название характеристики", example = "Русский")
    val name: String,

    @field:NotNull(message = "ID категории характеристики не может быть null")
    @Schema(description = "ID категории характеристики", example = "1")
    val idFeatureCategory: Int
)

@Schema(description = "Запрос на обновление простой характеристики")
data class UpdateFeaturePlainRequest(
    @field:Size(max = 255, message = "Название характеристики не может превышать 255 символов")
    @Schema(description = "Название характеристики", example = "Русский")
    val name: String?,

    @Schema(description = "ID категории характеристики", example = "1")
    val idFeatureCategory: Int?
)
