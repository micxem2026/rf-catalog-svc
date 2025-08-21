package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Вид ОИС")
data class OipSuperTypeDto(
    @Schema(description = "ID", example = "1") val id: Int,
    @Schema(description = "Наименование", example = "Видео") val name: String
)