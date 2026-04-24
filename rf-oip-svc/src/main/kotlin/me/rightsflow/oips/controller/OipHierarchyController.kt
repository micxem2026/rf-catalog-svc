package me.rightsflow.oips.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.entity.CustomPageResponse
import me.rightsflow.common.entity.toCustomResponse
import me.rightsflow.common.permission.annotation.RequiresPermission
import me.rightsflow.oips.dto.request.OipHierarchyCreateRequest
import me.rightsflow.oips.dto.response.OipHierarchyDto
import me.rightsflow.oips.service.OipHierarchyService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/oips/oip-hierarchy")
@Tag(name = "Иерархия ОИС", description = "Иерархическая подчинённость ОИС")
class OipHierarchyController(
    private val service: OipHierarchyService
) {
    @GetMapping("/{id}")
    @Operation(summary = "Получить запись иерархии по ID")
    @RequiresPermission("OipHierarchyController:GetOipHierarchyById", description = "Получение записи иерархии ОИС по ID")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun getById(@PathVariable id: Int): OipHierarchyDto = service.getById(id)

    @GetMapping("/children")
    @Operation(summary = "Получить список подчинённых ОИС по заданному ID_PARENT")
    @RequiresPermission("OipHierarchyController:GetChildrenByParent", description = "Получение списка подчинённых ОИС по ID_PARENT")
    @ApiResponse(responseCode = "200", description = "Список ОИС получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun getChildren(
        @RequestParam idParent: Int,
        @PageableDefault(size = 20) @ParameterObject pageable: Pageable
    ): CustomPageResponse<OipHierarchyDto> {
        val page = service.findChildrenByParent(idParent, pageable)
        return page.toCustomResponse()
    }

    @GetMapping("/parents")
    @Operation(summary = "Получить список родителей ОИС по заданному ID_OIP")
    @RequiresPermission("OipHierarchyController:GetParentsByOip", description = "Получение списка родительских ОИС по ID_OIP")
    @ApiResponse(responseCode = "200", description = "Список ОИС получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun getParents(
        @RequestParam idOip: Int,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): CustomPageResponse<OipHierarchyDto> {
        val page = service.findParentsByOip(idOip, pageable)
        return page.toCustomResponse()
    }

    @PostMapping
    @Operation(summary = "Создать запись иерархии (ins_klf_oip_hierarchy)")
    @RequiresPermission("OipHierarchyController:CreateOipHierarchy", description = "Создание записи иерархии ОИС")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "Запись иерархии ОИС создана")
    @ValidationErrorResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: OipHierarchyCreateRequest): OipHierarchyDto = service.create(req)

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись иерархии по заданному ID")
    @RequiresPermission("OipHierarchyController:DeleteOipHierarchy", description = "Удаление записи иерархии ОИС")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Запись иерархии ОИС удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) {
        service.delete(id)
    }
}