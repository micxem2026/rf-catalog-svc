package me.rightsflow.oips.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание ОИC")
data class OipCreateRequest(

    @field:Schema(description = "GUID", example = "014-12345678")
    @field:Size(max = 255)
    val guid: String?,

    @field:Schema(description = "ID вида ОИС", example = "1")
    @field:NotNull
    var idOipSuperType: Int,

    @field:Schema(description = "ID типа ОИС", example = "1")
    @field:NotNull
    var idOipType: Int,

    @field:Schema(description = "Название ОИС", example = "Москва слезам не верит")
    @field:NotBlank @field:Size(max = 512)
    val name: String,

    @field:Schema(description = "Номер ОИС в пределах типа", example = "0")
    val partNum: Int = 0,

    @field:Schema(description = "Количество дочерних ОИС", example = "0")
    val partCount: Int = 0,

    @field:Schema(description = "Длительность HH:mm:ss", example = "01:56:12")
    val duration: String? = null,

    @field:Schema(description = "Описание", example = "Описание ОИС")
    val description: String? = null,

    @field:Schema(description = "Оригинальное название")
    @field:Size(max = 512)
    val nativeName: String? = null,

    @field:Schema(description = "Год релиза")
    @field:Size(max = 50)
    val releaseYear: String? = null

)