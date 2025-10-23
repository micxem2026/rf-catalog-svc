package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Объект интеллектуальной собственности (ОИС)")
data class OipDto(
    @field:Schema(description = "ID", example = "1")
    val id: Int,
    @field:Schema(description = "GUID", example = "013-123456789")
    val guid: String?,
    @field:Schema(description = "ID вида ОИС", example = "1")
    val idOipSuperType: Int,
    @field:Schema(description = "ID типа ОИС", example = "1")
    val idOipType: Int,
    @field:Schema(description = "Название ОИС", example = "Москва слезам не верит")
    val name: String,
    @field:Schema(description = "Номер ОИС в пределах типа", example = "0")
    val partNum: Int,
    @field:Schema(description = "Количество дочерних ОИС", example = "0")
    val partCount: Int,
    @field:Schema(description = "Длительность в формате HH:mm:ss", example = "01:56:12")
    val duration: String?,
    @field:Schema(description = "Наименование вида ОИС", example = "Видео")
    val oipSuperTypeName: String?,
    @field:Schema(description = "Наименование типа ОИС", example = "Фильм")
    val oipTypeName: String?,
    @field:Schema(description = "Описание", example = "Описание ОИС")
    val description: String?,
    @field:Schema(description = "Наличие родителя у ОИС", example = "false")
    val hasParent: Boolean,
    @field:Schema(description = "Наличие потомков у ОИС", example = "false")
    val hasChildren: Boolean,
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