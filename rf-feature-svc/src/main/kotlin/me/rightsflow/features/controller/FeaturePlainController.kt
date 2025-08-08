package me.rightsflow.features.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.features.config.CommonSecurityResponses
import me.rightsflow.features.config.ConflictResponse
import me.rightsflow.features.config.InternalServerErrorResponse
import me.rightsflow.features.config.NotFoundResponse
import me.rightsflow.features.config.ValidationErrorResponse
import me.rightsflow.features.dto.request.CreateFeaturePlainRequest
import me.rightsflow.features.dto.request.UpdateFeaturePlainRequest
import me.rightsflow.features.dto.response.FeaturePlainResponse
import me.rightsflow.features.service.FeaturePlainService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/features/plains")
@Tag(name = "Feature Plains", description = "API для работы с простыми характеристиками")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "oauth2", scopes = ["read", "create", "update", "delete", "execute", "admin", "user", "manager"])
class FeaturePlainController(
    private val featurePlainService: FeaturePlainService
) {

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user')")
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
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @Operation(summary = "Получить список простых характеристик")
    @ApiResponse(responseCode = "200", description = "Список характеристик получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findAll(
        @Parameter(description = "ID категории для фильтрации") @RequestParam(required = true) categoryId: Int,
        @Parameter(description = "Имя для поиска") @RequestParam(required = false) name: String?,
        @PageableDefault(sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): Page<FeaturePlainResponse> {
        return featurePlainService.findAll(categoryId, name, pageable)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_create') or hasAuthority('SCOPE_manager')")
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
    @PreAuthorize("hasAuthority('SCOPE_update') or hasAuthority('SCOPE_manager')")
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
    @PreAuthorize("hasAuthority('SCOPE_delete') or hasAuthority('SCOPE_manager')")
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
