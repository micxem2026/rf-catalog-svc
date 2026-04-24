package me.rightsflow.righttypes.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.permission.annotation.RequiresPermission
import me.rightsflow.righttypes.dto.request.FeatureCatToRtCreateRequest
import me.rightsflow.righttypes.dto.request.FeatureCatToRtUpdateRequest
import me.rightsflow.righttypes.dto.response.FeatureCatToRtResponse
import me.rightsflow.righttypes.service.FeatureCatToRtService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/righttypes/fc2rt")
@Tag(name = "Feature Categories ↔ Right Types", description = "Категории характеристик для типов прав")
class FeatureCatToRtController(
    private val service: FeatureCatToRtService
) {

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по ID")
    @RequiresPermission("FeatureCatToRtController:GetFeatureCatToRtById", description = "Получить связь \"Тип права ↔ Категория\" по ID")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): FeatureCatToRtResponse = service.getById(id)

    @GetMapping
    @Operation(summary = "Список по ID_RIGHT_TYPE (постранично, sort по умолчанию — ID)")
    @RequiresPermission("FeatureCatToRtController:GetAllFeatureCatToRtByRightType", description = "Получить список связей \"Тип права ↔ Категория\"")
    @ApiResponse(responseCode = "200", description = "Список категорий получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findByRightType(
        @RequestParam("rightTypeId") rightTypeId: Int
    ): List<FeatureCatToRtResponse> = service.listByRightType(rightTypeId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать связь (категория ↔ тип права)")
    @RequiresPermission("FeatureCatToRtController:CreateFeatureCatToRt", description = "Создать связь \"Тип права ↔ Категория\"")
    @ApiResponse(responseCode = "201", description = "Запись создана")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: FeatureCatToRtCreateRequest): FeatureCatToRtResponse =
        service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Обновить запись по ID")
    @RequiresPermission("FeatureCatToRtController:UpdateFeatureCatToRt", description = "Обновить связь \"Тип права ↔ Категория\"")
    @ApiResponse(responseCode = "200", description = "Запись обновлена")
    @ValidationErrorResponse
    @NotFoundResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun update(@PathVariable id: Int, @Valid @RequestBody req: FeatureCatToRtUpdateRequest): FeatureCatToRtResponse =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить запись по ID")
    @RequiresPermission("FeatureCatToRtController:DeleteFeatureCatToRt", description = "Удалить связь \"Тип права ↔ Категория\"")
    @ApiResponse(responseCode = "204", description = "Запись удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) = service.delete(id)
}