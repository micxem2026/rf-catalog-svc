package me.rightsflow.righttypes.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание типа прав")
data class RightTypeCreateRequest(

    @field:Schema(description = "Родительский ID (NULL для корня).", example = "0")
    val idParent: Int? = null,

    @field:NotBlank(message = "Название типа права не может быть пустым")
    @field:Size(max = 255, message = "Название типа права не может превышать 255 символов")
    @field:Schema(description = "Название типа права", example = "FVOD")
    val name: String,

    @field:Size(max = 1023, message = "Описание типа права не может превышать 1023 символа")
    @field:Schema(description = "Описание типа права", example = "Бесплатное видео по запросу")
    val description: String?,

    @field:Schema(description = "ID группы прав", example = "4")
    val idRightGroup: Int
)

@Schema(description = "Запрос на изменение типа прав")
data class RightTypeUpdateRequest(

    @field:Schema(description = "Новый родитель (NULL допустим для корня)")
    val idParent: Int? = null,

    @field:Size(max = 255, message = "Название типа права не может превышать 255 символов")
    @field:Schema(description = "Новое имя. Если передать NULL, останется старое значение.")
    val name: String? = null,

    @field:Size(max = 1023, message = "Описание типа права не может превышать 1023 символа")
    @field:Schema(description = "Описание типа права", example = "Бесплатное видео по запросу")
    val description: String?,

    @field:Schema(description = "ID группы прав", example = "4")
    val idRightGroup: Int
)