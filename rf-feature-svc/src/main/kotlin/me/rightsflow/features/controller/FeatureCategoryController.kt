package me.rightsflow.features.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.permission.annotation.RequiresPermission
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/features/categories")
@Tag(name = "Feature Categories", description = "API для работы с категориями характеристик")
class FeatureCategoryController(
    private val featureCategoryService: FeatureCategoryService
) {

    @GetMapping("/{id}")
    @RequiresPermission("FeatureCategoryController:GetFeatureCategoryById", description = "Получение категории характеристики по ID")
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
    @RequiresPermission("FeatureCategoryController:GetAllFeatureCategories", description = "Получение списка категорий характеристик")
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
    @RequiresPermission("FeatureCategoryController:CreateFeatureCategory", description = "Создание новой категории характеристик")
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
    @RequiresPermission("FeatureCategoryController:UpdateFeatureCategory", description = "Обновление категории характеристик")
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
    @RequiresPermission("FeatureCategoryController:DeleteFeatureCategory", description = "Удаление категории характеристик")
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
