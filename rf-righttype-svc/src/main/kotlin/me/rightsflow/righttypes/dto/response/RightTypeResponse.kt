package me.rightsflow.righttypes.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Тип права")
data class RightTypeResponse(

    @field:Schema(description = "ID типа права", example = "1")
    val id: Int,

    @field:Schema(description = "ID родителя типа права", example = "null")
    val parentId: Int?,

    @field:Schema(description = "Название типа права", example = "FVOD")
    val name: String,

    @field:Schema(description = "Описание типа права", example = "Бесплатное видео по запросу")
    val description: String?,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: OffsetDateTime?
)