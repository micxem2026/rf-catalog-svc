package me.rightsflow.righttypes.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Тип права")
data class RightTypeResponse(

    @Schema(description = "ID типа права", example = "1")
    val id: Int,

    @Schema(description = "ID родителя типа права", example = "null")
    val parentId: Int?,

    @Schema(description = "Название типа права", example = "SVOD")
    val name: String,

    @Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @Schema(description = "Дата и время создания записи")
    val createdAt: OffsetDateTime,

    @Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: OffsetDateTime?
)