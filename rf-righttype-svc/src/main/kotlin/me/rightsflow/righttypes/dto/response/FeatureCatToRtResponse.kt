package me.rightsflow.righttypes.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Связь 'категория характеристик' ↔ 'тип права'")
data class FeatureCatToRtResponse(

    @Schema(description = "ID записи связи", example = "1")
    val id: Int,

    @Schema(description = "ID типа права", example = "1")
    val rightTypeId: Int,

    @Schema(description = "ID категории характеристики", example = "1")
    val featureCategoryId: Int,

    @Schema(description = "ID характеристики", example = "1")
    val defaultFeatureId: Int?,

    @Schema(description = "Название типа права", example = "Все права")
    val rightTypeName: String,

    @Schema(description = "Название категории характеристик", example = "Территория")
    val featureCategoryName: String,

    @Schema(description = "Название характеристики по умолчанию", example = "Весь мир")
    val defaultFeatureName: String?,

    @Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @Schema(description = "Дата и время создания записи")
    val createdAt: OffsetDateTime,

    @Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: OffsetDateTime?
)