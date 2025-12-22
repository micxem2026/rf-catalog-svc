package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Информация о родительском ОИС")
data class ParentInfo(
    @field:Schema(description = "ID родительского ОИС", example = "5")
    val id: Int,
    @field:Schema(description = "Название родительского ОИС", example = "Пакет 1")
    val name: String
)