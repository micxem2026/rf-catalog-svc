package me.rightsflow.oips.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * По правилам:
 * - Если поле NOT NULL и пришло null — НЕ изменяем (оставляем старое значение).
 * - Если поле nullable — записываем то, что пришло (включая null).
 * Поэтому все поля nullable.
 */
@Schema(description = "Запрос на обновление ОИС")
data class OipUpdateRequest(
    @Schema(description = "GUID")
    @Size(max = 255) val guid: String?,               // nullable -> обновляем если не null (GUID сам по себе nullable)

    @Schema(description = "ID вида ОИС")
    val idOipSuperType: Int?,                         // not null в БД -> null = не менять

    @Schema(description = "ID типа ОИС")
    val idOipType: Int?,                              // not null в БД -> null = не менять

    @Schema(description = "Название ОИС")
    @Size(max = 512) val name: String?,               // not null в БД -> null = не менять

    @Schema(description = "Номер ОИС в пределах типа", example = "0")
    val partNum: Int?,                                // not null в БД -> null = не менять

    @Schema(description = "Количество дочерних ОИС", example = "0")
    val partCount: Int?,                              // not null в БД -> null = не менять

    @Schema(description = "Длительность HH:mm:ss")
    val duration: String?                             // nullable -> может стать null
)