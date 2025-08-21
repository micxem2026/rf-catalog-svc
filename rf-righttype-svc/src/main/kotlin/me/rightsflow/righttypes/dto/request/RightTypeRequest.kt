package me.rightsflow.righttypes.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Запрос на создание типа прав")
data class RightTypeCreateRequest(

    @Schema(description = "Родительский ID (NULL для корня).", example = "0")
    val parentId: Int? = null,

    @field:NotBlank(message = "Название типа права не может быть пустым")
    @field:Size(max = 255, message = "Название типа права не может превышать 255 символов")
    @Schema(description = "Название типа права", example = "SVOD")
    val name: String
)

@Schema(description = "Запрос на изменение типа прав")
data class RightTypeUpdateRequest(

    @Schema(description = "Новый родитель (NULL допустим для корня)")
    val parentId: Int? = null,

    @field:Size(max = 255, message = "Название типа права не может превышать 255 символов")
    @Schema(description = "Новое имя. Если передать NULL, останется старое значение.")
    val name: String? = null
)