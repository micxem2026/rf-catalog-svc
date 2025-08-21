package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Тип ОИС")
data class OipTypeDto(
    @Schema(description = "ID", example = "1") val id: Int,
    @Schema(description = "ID супертипа", example = "1") val idOipSuperType: Int,
    @Schema(description = "Наименование", example = "Фильм") val name: String,
    @Schema(description = "Наименование супертипа", example = "Видео") val superTypeName: String
)