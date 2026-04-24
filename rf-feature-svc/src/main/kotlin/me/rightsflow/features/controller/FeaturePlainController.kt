package me.rightsflow.features.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.common.permission.annotation.RequiresPermission
import me.rightsflow.features.dto.request.CreateFeaturePlainRequest
import me.rightsflow.features.dto.request.UpdateFeaturePlainRequest
import me.rightsflow.features.dto.response.FeaturePlainResponse
import me.rightsflow.features.service.FeaturePlainService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/features/plains")
@Tag(name = "Feature Plains", description = "API для работы с простыми характеристиками")
class FeaturePlainController(
    private val featurePlainService: FeaturePlainService
) {

    @GetMapping("/{id}")
    @RequiresPermission("FeaturePlainController:GetFeaturePlainById", description = "Получение простой характеристики по ID")
    @Operation(summary = "Получить простую характеристику по ID")
    @ApiResponse(responseCode = "200", description = "Характеристика найдена")
    @NotFoundResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findById(
        @Parameter(description = "ID характеристики") @PathVariable id: Int
    ): FeaturePlainResponse {
        return featurePlainService.findById(id)
    }

    @GetMapping
    @RequiresPermission("FeaturePlainController:GetAllPlainFeatures", description = "Получение списка простых характеристик")
    @Operation(summary = "Получить список простых характеристик")
    @ApiResponse(responseCode = "200", description = "Список характеристик получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findAll(
        @Parameter(description = "ID категории для фильтрации") @RequestParam(required = true) categoryId: Int,
        @Parameter(description = "Имя для поиска") @RequestParam(required = false) name: String?,
        @PageableDefault(sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): PagedModel<FeaturePlainResponse> {
        return PagedModel(featurePlainService.findAll(categoryId, name, pageable))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("FeaturePlainController:CreatePlainFeature", description = "Создание простой характеристики")
    @Operation(summary = "Создать новую простую характеристику")
    @ApiResponse(responseCode = "201", description = "Характеристика создана")
    @CommonSecurityResponses
    @ValidationErrorResponse
    @InternalServerErrorResponse
    fun create(
        @Valid @RequestBody request: CreateFeaturePlainRequest,
        authentication: JwtAuthenticationToken
    ): FeaturePlainResponse {
        val userId = authentication.token.subject
        return featurePlainService.create(request, userId)
    }

    @PutMapping("/{id}")
    @RequiresPermission("FeaturePlainController:UpdatePlainFeature", description = "Обновление простой характеристики")
    @Operation(summary = "Обновить простую характеристику")
    @ApiResponse(responseCode = "200", description = "Характеристика обновлена")
    @ValidationErrorResponse
    @NotFoundResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun update(
        @Parameter(description = "ID характеристики") @PathVariable id: Int,
        @Valid @RequestBody request: UpdateFeaturePlainRequest,
        authentication: JwtAuthenticationToken
    ): FeaturePlainResponse {
        val userId = authentication.token.subject
        return featurePlainService.update(id, request, userId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("FeaturePlainController:DeletePlainFeature", description = "Удаление простой характеристики")
    @Operation(summary = "Удалить простую характеристику")
    @ApiResponse(responseCode = "204", description = "Характеристика удалена")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun deleteById(
        @Parameter(description = "ID характеристики") @PathVariable id: Int
    ) {
        featurePlainService.deleteById(id)
    }
}
