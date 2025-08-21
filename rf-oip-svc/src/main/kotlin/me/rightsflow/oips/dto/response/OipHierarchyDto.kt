package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Иерархия ОИС")
data class OipHierarchyDto(
    @Schema(description = "ID", example = "1")
    val id: Int,
    @Schema(description = "ID родительского ОИС", example = "1")
    val idParent: Int,
    @Schema(description = "ID ОИС", example = "1")
    val idOip: Int,
    @Schema(description = "Название родительского ОИС", example = "Пакет 1")
    val parentName: String,
    @Schema(description = "Название ОИС", example = "Москва слезам не верит")
    val oipName: String,
    // audit
    @Schema(description = "Пользователь, создавший запись", example = "admin")
    val createdBy: String,
    @Schema(description = "Дата и время создания записи", example = "2022-01-01T00:00:00Z")
    val createdAt: OffsetDateTime,
    @Schema(description = "Пользователь, обновивший запись", example = "admin")
    val updatedBy: String?,
    @Schema(description = "Дата и время обновления записи", example = "2022-01-01T00:00:00Z")
    val updatedAt: OffsetDateTime?
)