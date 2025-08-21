package me.rightsflow.righttypes.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.CommonSecurityResponses
import me.rightsflow.common.config.ConflictResponse
import me.rightsflow.common.config.InternalServerErrorResponse
import me.rightsflow.common.config.NotFoundResponse
import me.rightsflow.common.config.ValidationErrorResponse
import me.rightsflow.righttypes.dto.request.RightTypeCreateRequest
import me.rightsflow.righttypes.dto.request.RightTypeUpdateRequest
import me.rightsflow.righttypes.dto.response.RightTypeResponse
import me.rightsflow.righttypes.service.RightTypeService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Right Types", description = "Справочник типов прав")
@RestController
@RequestMapping("/righttypes")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "oauth2", scopes = ["read", "create", "update", "delete", "execute", "admin", "user", "manager"])
class RightTypeController(
    private val service: RightTypeService
) {

    @GetMapping("/{id}")
    @Operation(summary = "Получить тип права по ID")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): RightTypeResponse = service.getById(id)

    @GetMapping("/tree")
    @Operation(
        summary = "Получить дерево типов прав",
        description = "tree_mode: recursive — вложенные children; plain — плоский список с полем level (корень level=1)"
    )
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Дерево получено")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findTree(
        @Parameter(required = true, description = "plain | recursive")
        @RequestParam("tree_mode") treeMode: String
    ): Any = service.getTree(treeMode)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать тип права")
    @PreAuthorize("hasAnyAuthority('SCOPE_create','SCOPE_manager')")
    @ApiResponse(responseCode = "201", description = "Тип права создан")
    @ValidationErrorResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: RightTypeCreateRequest): RightTypeResponse =
        service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Изменить тип права по ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_update','SCOPE_manager')")
    @ApiResponse(responseCode = "200", description = "Характеристика обновлена")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @ConflictResponse
    @NotFoundResponse
    @InternalServerErrorResponse
    fun update(@PathVariable id: Int, @Valid @RequestBody req: RightTypeUpdateRequest): RightTypeResponse =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить тип права по ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_delete','SCOPE_manager')")
    @ApiResponse(responseCode = "204", description = "Характеристика удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) = service.delete(id)
}