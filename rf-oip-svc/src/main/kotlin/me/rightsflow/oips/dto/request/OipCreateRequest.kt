package me.rightsflow.oips.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание ОИC")
data class OipCreateRequest(

    @Schema(description = "GUID", example = "014-12345678")
    @Size(max = 255)
    val guid: String?,

    @Schema(description = "ID вида ОИС", example = "1")
    @field:NotNull
    val idOipSuperType: Int,

    @Schema(description = "ID типа ОИС", example = "1")
    @field:NotNull
    val idOipType: Int,

    @Schema(description = "Название ОИС", example = "Москва слезам не верит")
    @field:NotBlank @field:Size(max = 512)
    val name: String,

    @Schema(description = "Номер ОИС в пределах типа", example = "0")
    val partNum: Int = 0,

    @Schema(description = "Количество дочерних ОИС", example = "0")
    val partCount: Int = 0,

    @Schema(description = "Длительность HH:mm:ss", example = "01:56:12")
    val duration: String? = null
)