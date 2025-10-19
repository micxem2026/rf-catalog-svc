package me.rightsflow.features.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание категории характеристик")
data class CreateFeatureCategoryRequest(
    @field:NotBlank(message = "Название категории не может быть пустым")
    @field:Size(max = 50, message = "Название категории не может превышать 50 символов")
    @field:Schema(description = "Название категории", example = "Язык")
    val name: String
)

@Schema(description = "Запрос на обновление категории характеристик")
data class UpdateFeatureCategoryRequest(
    @field:Size(max = 50, message = "Название категории не может превышать 50 символов")
    @field:Schema(description = "Название категории", example = "Язык")
    val name: String?
)
