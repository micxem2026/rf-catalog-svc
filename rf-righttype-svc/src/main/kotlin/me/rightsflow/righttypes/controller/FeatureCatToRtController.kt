package me.rightsflow.righttypes.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.CommonSecurityResponses
import me.rightsflow.common.config.ConflictResponse
import me.rightsflow.common.config.InternalServerErrorResponse
import me.rightsflow.common.config.NotFoundResponse
import me.rightsflow.common.config.ValidationErrorResponse
import me.rightsflow.righttypes.dto.request.FeatureCatToRtCreateRequest
import me.rightsflow.righttypes.dto.request.FeatureCatToRtUpdateRequest
import me.rightsflow.righttypes.dto.response.FeatureCatToRtResponse
import me.rightsflow.righttypes.service.FeatureCatToRtService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Feature Categories ↔ Right Types", description = "Категории характеристик для типов прав")
@RestController
@RequestMapping("/righttypes/fc2rt")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "oauth2", scopes = ["read", "create", "update", "delete", "execute", "admin", "user", "manager"])
class FeatureCatToRtController(
    private val service: FeatureCatToRtService
) {

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по ID")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): FeatureCatToRtResponse = service.getById(id)

    @GetMapping
    @Operation(summary = "Список по ID_RIGHT_TYPE (постранично, sort по умолчанию — ID)")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список категорий получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findByRightType(
        @RequestParam("rightTypeId") rightTypeId: Int
    ): List<FeatureCatToRtResponse> = service.listByRightType(rightTypeId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать связь (категория ↔ тип права)")
    @PreAuthorize("hasAnyAuthority('SCOPE_create','SCOPE_manager')")
    @ApiResponse(responseCode = "201", description = "Запись создана")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: FeatureCatToRtCreateRequest): FeatureCatToRtResponse =
        service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Обновить запись по ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_update','SCOPE_manager')")
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
    @PreAuthorize("hasAnyAuthority('SCOPE_delete','SCOPE_manager')")
    @ApiResponse(responseCode = "204", description = "Запись удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) = service.delete(id)
}