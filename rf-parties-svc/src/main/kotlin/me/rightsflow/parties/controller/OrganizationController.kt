package me.rightsflow.parties.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.permission.annotation.RequiresPermission
import me.rightsflow.parties.dto.request.OrganizationCreateRequest
import me.rightsflow.parties.dto.request.OrganizationUpdateRequest
import me.rightsflow.parties.dto.response.OrganizationDto
import me.rightsflow.parties.service.OrganizationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/parties/organizations")
@Tag(name = "Организации", description = "Операции с организациями")
class OrganizationController(
    private val service: OrganizationService
) {
    @GetMapping("/{id}")
    @Operation(summary = "Получить организацию по ID")
    @RequiresPermission("OrganizationController:GetOrganizationById", description = "Получить организацию по ID")
    @ApiResponse(responseCode = "200", description = "Организация найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): OrganizationDto = service.getById(id)

    @GetMapping
    @Operation(summary = "Поиск организаций по фильтру названия и guid (с пагинацией)")
    @RequiresPermission("OrganizationController:FindAllOrganizationsByFilter", description = "Поиск организаций по фильтру (с пагинацией)")
    @ApiResponse(responseCode = "200", description = "Список организаций получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findByFilter(
        @RequestParam(required = false) filter: String?,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): PagedModel<OrganizationDto> {
        val page = service.findByFilter(filter, pageable)
        return PagedModel(page)
    }

    @PostMapping
    @Operation(summary = "Создать новою организацию")
    @RequiresPermission("OrganizationController:CreateOrganization", description = "Создать организацию")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "Организация создана")
    @ValidationErrorResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: OrganizationCreateRequest): OrganizationDto = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Изменить организацию по заданному ID")
    @RequiresPermission("OrganizationController:UpdateOrganization", description = "Изменить организацию")
    @ApiResponse(responseCode = "200", description = "Организация обновлена")
    @ValidationErrorResponse
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun update(@PathVariable id: Int, @Valid @RequestBody req: OrganizationUpdateRequest): OrganizationDto =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить организацию по заданному ID")
    @RequiresPermission("OrganizationController:DeleteOrganization", description = "Удалить организацию")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Организация удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) {
        service.delete(id)
    }
}