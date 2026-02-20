package me.rightsflow.parties.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание контрагента")
data class CounterpartyCreateRequest(

    @field:Schema(description = "GUID", example = "014-12345678")
    @field:Size(max = 255)
    val guid: String?,

    @field:Schema(description = "Код 1С", example = "Н014-123")
    @field:Size(max = 50)
    val code1c: String?,

    @field:Schema(description = "Страна контрагента", example = "Россия")
    @field:Size(max = 32)
    val country: String?,

    @field:Schema(description = "Адрес контрагента", example = "111222, г.Москва, ул. Правды, дом 101, строение 6")
    val address: String?,

    @field:Schema(description = "ИНН контрагента", example = "111111111111")
    @field:Size(max = 32)
    val tin: String?,

    @field:Schema(description = "Название контрагента", example = "ООО \"Рога и копыта\"")
    @field:NotBlank @field:Size(max = 255)
    val name: String,

    @field:Schema(description = "Идентификатор организации связанной с контрагентом", example = "1")
    val idOrgRef: Int?

)

@Schema(description = "Запрос на изменение контрагента")
data class CounterpartyUpdateRequest(

    @field:Schema(description = "GUID", example = "014-12345678")
    @field:Size(max = 255)
    val guid: String?,

    @field:Schema(description = "Код 1С", example = "Н014-123")
    @field:Size(max = 50)
    val code1c: String?,

    @field:Schema(description = "Страна контрагента", example = "Россия")
    @field:Size(max = 32)
    val country: String?,

    @field:Schema(description = "Адрес контрагента", example = "111222, г.Москва, ул. Правды, дом 101, строение 6")
    val address: String?,

    @field:Schema(description = "ИНН контрагента", example = "111111111111")
    @field:Size(max = 32)
    val tin: String?,

    @field:Schema(description = "Название контрагента", example = "ООО \"Рога и копыта\"")
    @field:Size(max = 255)
    val name: String?,

    @field:Schema(description = "Идентификатор организации связанной с контрагентом", example = "1")
    val idOrgRef: Int?

)