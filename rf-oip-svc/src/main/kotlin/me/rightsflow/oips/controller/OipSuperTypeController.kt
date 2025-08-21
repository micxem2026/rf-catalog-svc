package me.rightsflow.oips.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import me.rightsflow.common.config.CommonSecurityResponses
import me.rightsflow.common.config.InternalServerErrorResponse
import me.rightsflow.common.config.NotFoundResponse
import me.rightsflow.oips.dto.response.OipSuperTypeDto
import me.rightsflow.oips.service.OipSuperTypeService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/oips/oip-super-types")
@Tag(name = "Виды ОИС", description = "Список видов ОИС")
class OipSuperTypeController(
    private val service: OipSuperTypeService,
) {
    @GetMapping("/{id}")
    @Operation(summary = "Получить вид ОИС по ID")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Запись найдена")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): OipSuperTypeDto = service.getById(id)

    @GetMapping
    @Operation(summary = "Получить список всех видов ОИС")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список всех видов ОИС получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findAll(): List<OipSuperTypeDto> = service.findAll()
}