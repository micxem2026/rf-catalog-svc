package me.rightsflow.righttypes.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Узел дерева типов прав (recursive)")
data class RightTypeTreeNode(

    @field:Schema(description = "ID типа права", example = "1")
    val id: Int,

    @field:Schema(description = "ID родителя типа права", example = "null")
    val idParent: Int?,

    @field:Schema(description = "Название типа права", example = "FVOD")
    val name: String,

    @field:Schema(description = "Описание типа права", example = "Бесплатное видео по запросу")
    val description: String?,

    @field:Schema(description = "ID группы прав", example = "4")
    val idRightGroup: Int,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: OffsetDateTime?,

    @field:Schema(description = "Список дочерних элементов")
    val children: List<RightTypeTreeNode>
)

@Schema(description = "Элемент плоского дерева типов прав (plain)")
data class RightTypePlainItem(

    @field:Schema(description = "ID типа права", example = "1")
    val id: Int,

    @field:Schema(description = "ID родителя типа права", example = "null")
    val parentId: Int?,

    @field:Schema(description = "Название типа права", example = "FVOD")
    val name: String,

    @field:Schema(description = "Описание типа права", example = "Бесплатное видео по запросу")
    val description: String?,

    @field:Schema(description = "ID группы прав", example = "4")
    val idRightGroup: Int,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    val createdAt: OffsetDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    val updatedAt: OffsetDateTime?,

    @field:Schema(description = "Уровень вложенности (начинается с 1)", example = "1")
    val level: Int
)