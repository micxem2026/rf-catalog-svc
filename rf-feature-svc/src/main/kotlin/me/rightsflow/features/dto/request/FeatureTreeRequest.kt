package me.rightsflow.features.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(description = "Запрос на создание характеристики в дереве")
data class CreateFeatureTreeRequest(
    @Schema(description = "ID родительского элемента", example = "1")
    val idParent: Int?,

    @field:NotNull(message = "ID простой характеристики не может быть null")
    @Schema(description = "ID простой характеристики", example = "5")
    val idFeaturePlain: Int,

    @Schema(description = "Дата начала периода действия элемента. Если указан NULL, то дата начала периода не имеет ограничения снизу", example = "2022-01-01")
    val beginDate: LocalDate?,

    @Schema(description = "Дата окончания периода действия элемента. Если указан NULL, то дата окончания периода не имеет ограничения сверху", example = "2025-01-01")
    val endDate: LocalDate?
)

@Schema(description = "Запрос на обновление характеристики в дереве")
data class UpdateFeatureTreeRequest(
    @Schema(description = "ID родительского элемента", example = "1")
    val idParent: Int?,

    @Schema(description = "ID простой характеристики", example = "5")
    val idFeaturePlain: Int?,

    @Schema(description = "Дата начала периода действия элемента. Если указан NULL, то дата начала периода не имеет ограничения снизу", example = "2022-01-01")
    val beginDate: LocalDate?,

    @Schema(description = "Дата окончания периода действия элемента. Если указан NULL, то дата окончания периода не имеет ограничения сверху", example = "2025-01-01")
    val endDate: LocalDate?
)
