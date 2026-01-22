package me.rightsflow.righttypes.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Связь 'категория характеристик' ↔ 'тип права'")
data class FeatureCatToRtResponse(

    @field:Schema(description = "ID записи связи", example = "1")
    val id: Int,

    @field:Schema(description = "ID типа права", example = "1")
    val idRightType: Int,

    @field:Schema(description = "ID категории характеристики", example = "1")
    val idFeatureCategory: Int,

    @field:Schema(description = "ID характеристики", example = "1")
    val idDefaultFeature: Int?,

    @field:Schema(description = "Название типа права", example = "Все права")
    val rightTypeName: String,

    @field:Schema(description = "Название категории характеристик", example = "Территория")
    val featureCategoryName: String,

    @field:Schema(description = "Название характеристики по умолчанию", example = "Весь мир")
    val defaultFeatureName: String?,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: OffsetDateTime?
)