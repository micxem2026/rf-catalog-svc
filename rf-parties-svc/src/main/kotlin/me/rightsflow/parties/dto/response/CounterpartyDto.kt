package me.rightsflow.parties.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Контрагент")
data class CounterpartyDto(
    @Schema(description = "ID", example = "1")
    val id: Int,
    @Schema(description = "GUID", example = "013-123456789")
    val guid: String?,
    @Schema(description = "Название контрагента", example = "ООО \"Рога и копыта\"")
    val name: String,
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