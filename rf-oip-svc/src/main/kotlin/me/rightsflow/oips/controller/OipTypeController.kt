package me.rightsflow.oips.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import me.rightsflow.common.config.CommonSecurityResponses
import me.rightsflow.common.config.InternalServerErrorResponse
import me.rightsflow.common.config.NotFoundResponse
import me.rightsflow.oips.dto.response.OipTypeDto
import me.rightsflow.oips.service.OipTypeService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/oips/oip-types")
@Tag(name = "Типы ОИС", description = "Список типов ОИС")
class OipTypeController(
    private val service: OipTypeService
) {
    @GetMapping("/{id}")
    @Operation(summary = "Получить тип ОИС по ID")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): OipTypeDto = service.getById(id)

    @GetMapping
    @Operation(summary = "Получить список всех типов ОИС")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список всех типов ОИС получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findAll(): List<OipTypeDto> = service.findAll()
}