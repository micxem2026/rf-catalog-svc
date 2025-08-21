package me.rightsflow.features.controller

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
import me.rightsflow.features.dto.request.CreateFeatureTreeRequest
import me.rightsflow.features.dto.request.UpdateFeatureTreeRequest
import me.rightsflow.features.dto.response.FeatureTreeProjection
import me.rightsflow.features.service.FeatureTreeService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/features/trees")
@Tag(name = "Feature Trees", description = "API для работы с деревом характеристик")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(
    name = "oauth2",
    scopes = ["read", "create", "update", "delete", "execute", "admin", "user", "manager"]
)
class FeatureTreeController(
    private val featureTreeService: FeatureTreeService
) {

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @Operation(summary = "Получить характеристику из дерева по ID")
    @ApiResponse(responseCode = "200", description = "Характеристика найдена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findById(
        @Parameter(description = "ID характеристики") @PathVariable id: Int
    ): FeatureTreeProjection {
        return featureTreeService.findById(id)
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @Operation(summary = "Получить дерево характеристик по категории")
    @ApiResponse(responseCode = "200", description = "Дерево получено")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findTreeByCategory(
        @Parameter(description = "ID категории") @PathVariable categoryId: Int,
        @Parameter(description = "Режим вывода дерева: 'recursive' или 'plain'")
        @RequestParam("tree_mode") treeMode: String
    ): Any {
        return featureTreeService.findTreeByCategory(categoryId, treeMode)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_create') or hasAuthority('SCOPE_manager')")
    @Operation(summary = "Создать новую характеристику в дереве")
    @ApiResponse(responseCode = "201", description = "Характеристика создана")
    @ValidationErrorResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(
        @Valid @RequestBody request: CreateFeatureTreeRequest,
        authentication: JwtAuthenticationToken
    ): FeatureTreeProjection {
        val userId = authentication.token.subject
        return featureTreeService.create(request, userId)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_update') or hasAuthority('SCOPE_manager')")
    @Operation(summary = "Обновить характеристику в дереве")
    @ApiResponse(responseCode = "200", description = "Характеристика обновлена")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @ConflictResponse
    @NotFoundResponse
    @InternalServerErrorResponse
    fun update(
        @Parameter(description = "ID характеристики") @PathVariable id: Int,
        @Valid @RequestBody request: UpdateFeatureTreeRequest,
        authentication: JwtAuthenticationToken
    ): FeatureTreeProjection {
        val userId = authentication.token.subject
        return featureTreeService.update(id, request, userId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_delete') or hasAuthority('SCOPE_manager')")
    @Operation(summary = "Удалить характеристику из дерева")
    @ApiResponse(responseCode = "204", description = "Характеристика удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun deleteById(
        @Parameter(description = "ID характеристики") @PathVariable id: Int
    ) {
        featureTreeService.deleteById(id)
    }
}
