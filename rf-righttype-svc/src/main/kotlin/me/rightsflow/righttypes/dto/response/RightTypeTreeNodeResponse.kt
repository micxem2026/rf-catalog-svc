package me.rightsflow.righttypes.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Узел дерева типов прав (recursive)")
data class RightTypeTreeNode(

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
    val updatedAt: OffsetDateTime?,

    @Schema(description = "Список дочерних элементов")
    val children: List<RightTypeTreeNode>
)

@Schema(description = "Элемент плоского дерева типов прав (plain)")
data class RightTypePlainItem(

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
    val updatedAt: OffsetDateTime?,

    @Schema(description = "Уровень вложенности (начинается с 1)", example = "1")
    val level: Int
)