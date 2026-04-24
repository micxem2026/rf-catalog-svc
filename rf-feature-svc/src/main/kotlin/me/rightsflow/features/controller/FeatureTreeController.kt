package me.rightsflow.features.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.permission.annotation.RequiresPermission
import me.rightsflow.features.dto.request.CreateFeatureTreeRequest
import me.rightsflow.features.dto.request.UpdateFeatureTreeRequest
import me.rightsflow.features.dto.response.FeatureTreeProjection
import me.rightsflow.features.service.FeatureTreeService
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/features/trees")
@Tag(name = "Feature Trees", description = "API для работы с деревом характеристик")
class FeatureTreeController(
    private val featureTreeService: FeatureTreeService
) {

    @GetMapping("/{id}")
    @RequiresPermission("FeatureTreeController:GetFeatureTreeById", description = "Получение характеристики из дерева по ID")
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
    @RequiresPermission("FeatureTreeController:GetFeatureTreeByCategoryId", description = "Получение дерева характеристик по категории")
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
    @RequiresPermission("FeatureTreeController:CreateFeatureTree", description = "Создание характеристики в дереве характеристик")
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
    @RequiresPermission("FeatureTreeController:UpdateFeatureTree", description = "Обновление характеристики в дереве")
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
    @RequiresPermission("FeatureTreeController:DeleteFeatureTree", description = "Удаление характеристики из дерева")
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
