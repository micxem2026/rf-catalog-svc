package me.rightsflow.parties.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание контрагента")
data class CounterpartyCreateRequest(

    @field:Schema(description = "GUID", example = "014-12345678")
    @field:Size(max = 255)
    val guid: String?,

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

    @field:Schema(description = "Название контрагента", example = "ООО \"Рога и копыта\"")
    @field:Size(max = 255)
    val name: String?,

    @field:Schema(description = "Идентификатор организации связанной с контрагентом", example = "1")
    val idOrgRef: Int?

)