package me.rightsflow.oips.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Объект интеллектуальной собственности (ОИС)")
data class OipDto(
    @Schema(description = "ID", example = "1")
    val id: Int,
    @Schema(description = "GUID", example = "013-123456789")
    val guid: String?,
    @Schema(description = "ID вида ОИС", example = "1")
    val idOipSuperType: Int,
    @Schema(description = "ID типа ОИС", example = "1")
    val idOipType: Int,
    @Schema(description = "Название ОИС", example = "Москва слезам не верит")
    val name: String,
    @Schema(description = "Номер ОИС в пределах типа", example = "0")
    val partNum: Int,
    @Schema(description = "Количество дочерних ОИС", example = "0")
    val partCount: Int,
    @Schema(description = "Длительность в формате HH:mm:ss", example = "01:56:12")
    val duration: String?,
    @Schema(description = "Наименование вида ОИС", example = "Видео")
    val oipSuperTypeName: String?,
    @Schema(description = "Наименование типа ОИС", example = "Фильм")
    val oipTypeName: String?,
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