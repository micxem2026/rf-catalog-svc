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
import me.rightsflow.features.dto.request.CreateFeatureCategoryRequest
import me.rightsflow.features.dto.request.UpdateFeatureCategoryRequest
import me.rightsflow.features.dto.response.FeatureCategoryResponse
import me.rightsflow.features.service.FeatureCategoryService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/features/categories")
@Tag(name = "Feature Categories", description = "API для работы с категориями характеристик")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(
    name = "oauth2",
    scopes = ["read", "create", "update", "delete", "execute", "admin", "user", "manager"]
)
class FeatureCategoryController(
    private val featureCategoryService: FeatureCategoryService
) {

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @Operation(summary = "Получить категорию характеристик по ID")
    @ApiResponse(responseCode = "200", description = "Категория найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(
        @Parameter(description = "ID категории") @PathVariable id: Int
    ): FeatureCategoryResponse {
        return featureCategoryService.findById(id)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @Operation(summary = "Получить список категорий характеристик")
    @ApiResponse(responseCode = "200", description = "Список категорий получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findAll(
        @PageableDefault(sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): PagedModel<FeatureCategoryResponse> {
        return PagedModel(featureCategoryService.findAll(pageable))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_create') or hasAuthority('SCOPE_manager')")
    @Operation(summary = "Создать новую категорию характеристик")
    @ApiResponse(responseCode = "201", description = "Категория создана")
    @ValidationErrorResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(
        @Valid @RequestBody request: CreateFeatureCategoryRequest,
        authentication: JwtAuthenticationToken
    ): FeatureCategoryResponse {
        val userId = authentication.token.subject
        return featureCategoryService.create(request, userId)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_update') or hasAuthority('SCOPE_manager')")
    @Operation(summary = "Обновить категорию характеристик")
    @ApiResponse(responseCode = "200", description = "Категория обновлена")
    @ValidationErrorResponse
    @NotFoundResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun update(
        @Parameter(description = "ID категории") @PathVariable id: Int,
        @Valid @RequestBody request: UpdateFeatureCategoryRequest,
        authentication: JwtAuthenticationToken
    ): FeatureCategoryResponse {
        val userId = authentication.token.subject
        return featureCategoryService.update(id, request, userId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_delete') or hasAuthority('SCOPE_manager')")
    @Operation(summary = "Удалить категорию характеристик")
    @ApiResponse(responseCode = "204", description = "Категория удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun deleteById(
        @Parameter(description = "ID категории") @PathVariable id: Int
    ) {
        featureCategoryService.deleteById(id)
    }
}
