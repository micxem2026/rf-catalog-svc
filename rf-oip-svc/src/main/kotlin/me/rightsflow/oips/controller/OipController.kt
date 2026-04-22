package me.rightsflow.oips.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.entity.CustomPageResponse
import me.rightsflow.common.entity.toCustomResponse
import me.rightsflow.oips.dto.request.OipCreateRequest
import me.rightsflow.oips.dto.request.OipUpdateRequest
import me.rightsflow.oips.dto.response.OipDto
import me.rightsflow.oips.entity.Oip
import me.rightsflow.oips.service.OipService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/oips")
@Tag(name = "ОИС", description = "Операции с объектами интеллектуальной собственности (ОИС)")
class OipController(
    private val service: OipService
) {
    @GetMapping("/{id}")
    @Operation(summary = "Получить ОИС по ID")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "ОИС найден")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): OipDto = service.getById(id)

    @GetMapping
    @Operation(summary = "Поиск ОИС по фильтрам (с пагинацией)")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список ОИС получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findByFilter(
        @RequestParam(required = false) idOipSuperType: Int?,
        @RequestParam(required = false) idOipType: Int?,
        @Parameter(name = "nodeType", schema = Schema(type = "string", allowableValues = ["ROOT", "LEAF", "BRANCH", "ISOLATED"]))
        @RequestParam(required = false) nodeType: String?,
        @RequestParam(required = false) filter: String?,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): CustomPageResponse<OipDto> {
        val nType = when (nodeType) {
            "ROOT" -> Oip.NodeType.ROOT
            "LEAF" -> Oip.NodeType.LEAF
            "BRANCH" -> Oip.NodeType.BRANCH
            "ISOLATED" -> Oip.NodeType.ISOLATED
            else -> null
        }
        val page = service.findByFilter(idOipSuperType, idOipType, nType, filter, pageable)
        return page.toCustomResponse()
    }

    @GetMapping("/{id}/hierarchy")
    @Operation(
        summary = "Получить все ОИС в иерархии заданного ОИС",
        description = """
            Возвращает все объекты ОИС, связанные с заданным через иерархию.
            Параметр direction позволяет выбрать направление обхода:
            - UP: только предки (родители вверх по иерархии)
            - DOWN: только потомки (дети вниз по иерархии)  
            - BOTH: полная иерархия (и предки, и потомки)
            
            Внимание: метод возвращает все элементы иерархии без пагинации,
            чтобы сохранить целостность дерева связей.
        """
    )
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список всех ОИС из иерархии получен")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun getOipHierarchy(
        @Parameter(description = "ID ОИС", required = true, example = "1")
        @PathVariable id: Int,
        @Parameter(
            description = "Направление обхода иерархии",
            schema = Schema(type = "string", allowableValues = ["UP", "DOWN", "BOTH"])
        )
        @RequestParam(required = false, defaultValue = "BOTH") direction: String?
    ): List<OipDto> {
        val dir = when (direction?.uppercase()) {
            "UP" -> Oip.HierarchyDirection.UP
            "DOWN" -> Oip.HierarchyDirection.DOWN
            else -> Oip.HierarchyDirection.BOTH
        }
        return service.findAllInHierarchy(id, dir)
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Получить список подчинённых ОИС по заданному ID ОИС родителя")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список ОИС получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun getChildren(
        @Parameter(description = "ID ОИС родителя", required = true, example = "1")
        @PathVariable id: Int,
        @PageableDefault(size = 20) @ParameterObject pageable: Pageable
    ): CustomPageResponse<OipDto> {
        val page = service.findChildrenByParent(id, pageable)
        return page.toCustomResponse()
    }

    @PostMapping
    @Operation(summary = "Создать новый ОИС")
    @PreAuthorize("hasAnyAuthority('SCOPE_create','SCOPE_manager')")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "ОИС создан")
    @ValidationErrorResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: OipCreateRequest): OipDto = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Изменить ОИС по заданному ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_update','SCOPE_manager')")
    @ApiResponse(responseCode = "200", description = "ОИС обновлён")
    @ValidationErrorResponse
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun update(@PathVariable id: Int, @Valid @RequestBody req: OipUpdateRequest): OipDto = service.update(id, req)

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить ОИС по заданному ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_delete','SCOPE_manager')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "ОИС удалён")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) {
        service.delete(id)
    }
}