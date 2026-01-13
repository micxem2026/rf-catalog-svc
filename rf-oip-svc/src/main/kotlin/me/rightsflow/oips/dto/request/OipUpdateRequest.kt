package me.rightsflow.oips.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import me.rightsflow.pge.dto.ShortPropertyUpdateBatchDto

/**
 * По правилам:
 * - Если поле NOT NULL и пришло null — НЕ изменяем (оставляем старое значение).
 * - Если поле nullable — записываем то, что пришло (включая null).
 * Поэтому все поля nullable.
 */
@Schema(description = "Запрос на обновление ОИС")
data class OipUpdateRequest(
    @field:Schema(description = "GUID")
    @field:Size(max = 255) val guid: String?,         // nullable -> обновляем если не null (GUID сам по себе nullable)

    @field:Schema(description = "ID вида ОИС")
    val idOipSuperType: Int?,                         // not null в БД -> null = не менять

    @field:Schema(description = "ID типа ОИС")
    val idOipType: Int?,                              // not null в БД -> null = не менять

    @field:Schema(description = "Название ОИС")
    @field:Size(max = 512) val name: String?,         // not null в БД -> null = не менять

    @field:Schema(description = "Номер ОИС в пределах типа", example = "0")
    val partNum: Int?,                                // not null в БД -> null = не менять

    @field:Schema(description = "Количество дочерних ОИС", example = "0")
    val partCount: Int?,                              // not null в БД -> null = не менять

    @field:Schema(description = "Длительность HH:mm:ss")
    val duration: String?,                             // nullable -> может стать null

    @field:Schema(description = "Описание", example = "Описание ОИС")
    val description: String?,                          // nullable -> может стать null

    @field:Schema(description = "Оригинальное название")
    @field:Size(max = 512)
    val nativeName: String? = null,

    @field:Schema(description = "Полное название ОИС")
    @field:Size(max = 512)
    val fullName: String? = null,

    @field:Schema(description = "Год релиза")
    @field:Size(max = 50)
    val releaseYear: String? = null,

    @field:Schema(description = "Данные для обновления свойств" )
    val propsUpdate: List<ShortPropertyUpdateBatchDto> = emptyList()
)