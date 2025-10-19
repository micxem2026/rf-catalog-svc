package me.rightsflow.features.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Ответ с данными простой характеристики")
data class FeaturePlainResponse(
    @field:Schema(description = "ID характеристики", example = "1")
    val id: Int,

    @field:Schema(description = "Название характеристики", example = "Русский")
    val name: String,

    @field:Schema(description = "ID категории характеристики", example = "1")
    val idFeatureCategory: Int,

    @field:Schema(description = "Название категории характеристики", example = "Язык")
    val categoryName: String,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    val createdAt: LocalDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: LocalDateTime?
)
