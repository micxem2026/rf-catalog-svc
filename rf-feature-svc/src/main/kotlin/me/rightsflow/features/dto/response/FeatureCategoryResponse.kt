package me.rightsflow.features.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Ответ с данными категории характеристик")
data class FeatureCategoryResponse(
    @Schema(description = "ID категории", example = "1")
    val id: Int,

    @Schema(description = "Название категории", example = "Язык")
    val name: String,

    @Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @Schema(description = "Дата и время создания записи")
    val createdAt: LocalDateTime,

    @Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: LocalDateTime?
)
