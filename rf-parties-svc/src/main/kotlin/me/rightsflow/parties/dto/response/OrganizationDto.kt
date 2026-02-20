package me.rightsflow.parties.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Организация")
data class OrganizationDto(
    @field:Schema(description = "ID", example = "1")
    val id: Int,
    @field:Schema(description = "GUID", example = "013-123456789")
    val guid: String?,
    @field:Schema(description = "Код 1С", example = "Н013-123")
    val code1c: String?,
    @field:Schema(description = "Название организации", example = "ООО \"Рога и копыта\"")
    val name: String,
    @field:Schema(description = "Страна контрагента", example = "Россия")
    val country: String?,
    @field:Schema(description = "Адрес контрагента", example = "111222, г.Москва, ул. Правды, дом 101, строение 6")
    val address: String?,
    @field:Schema(description = "ИНН контрагента", example = "111111111111")
    val tin: String?,
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