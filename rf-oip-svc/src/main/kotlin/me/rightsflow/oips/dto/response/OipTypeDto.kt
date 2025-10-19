package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Тип ОИС")
data class OipTypeDto(
    @field:Schema(description = "ID", example = "1") val id: Int,
    @field:Schema(description = "ID супер типа", example = "1") val idOipSuperType: Int,
    @field:Schema(description = "Наименование", example = "Фильм") val name: String,
    @field:Schema(description = "Наименование супер типа", example = "Видео") val superTypeName: String
)