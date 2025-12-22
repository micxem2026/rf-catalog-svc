package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Иерархия ОИС")
data class OipHierarchyDto(
    @field:Schema(description = "ID", example = "1")
    val id: Int,
    @field:Schema(description = "ID родительского ОИС", example = "1")
    val idParent: Int,
    @field:Schema(description = "ID ОИС", example = "1")
    val idOip: Int,
    @field:Schema(description = "Название родительского ОИС", example = "Пакет 1")
    val parentName: String,
    @field:Schema(description = "Название ОИС", example = "Москва слезам не верит")
    val name: String,
    @field:Schema(description = "Наличие родителя у ОИС", example = "false")
    val hasParent: Boolean,
    @field:Schema(description = "Наличие потомков у ОИС", example = "false")
    val hasChildren: Boolean,
    @field:Schema(description = "Количество потомков у ОИС", example = "0")
    val childrenCount: Int,
    // audit
    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,
    @field:Schema(description = "Дата и время создания записи", example = "2022-01-01T00:00:00Z")
    val createdAt: OffsetDateTime,
    @field:Schema(description = "Пользователь, обновивший запись", example = "admin")
    val updatedBy: String?,
    @field:Schema(description = "Дата и время обновления записи", example = "2022-01-01T00:00:00Z")
    val updatedAt: OffsetDateTime?
)