package me.rightsflow.righttypes.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Запрос на создание связи 'категория характеристик' ↔ 'тип права'")
data class FeatureCatToRtCreateRequest(

    @field:NotNull(message = "ID типа права не может быть null")
    @Schema(description = "ID типа права", example = "1")
    val rightTypeId: Int,

    @field:NotNull(message = "ID категории характеристики не может быть null")
    @Schema(description = "ID категории характеристики", example = "1")
    val featureCategoryId: Int,

    @Schema(description = "ID характеристики", example = "1")
    val defaultFeatureId: Int? = null
)

@Schema(description = "Запрос на изменение связи 'категория характеристик' ↔ 'тип права'")
data class FeatureCatToRtUpdateRequest(

    @Schema(description = "ID типа права. Если передать NULL, останется старое значение.", example = "1")
    val rightTypeId: Int? = null,

    @Schema(description = "ID категории характеристики. Если передать NULL, останется старое значение.", example = "1")
    val featureCategoryId: Int? = null,

    @Schema(description = "ID категории характеристики", example = "1")
    val defaultFeatureId: Int? = null
)