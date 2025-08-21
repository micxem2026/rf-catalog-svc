package me.rightsflow.parties.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.common.config.*
import me.rightsflow.parties.dto.request.CounterpartyCreateRequest
import me.rightsflow.parties.dto.request.CounterpartyUpdateRequest
import me.rightsflow.parties.dto.response.CounterpartyDto
import me.rightsflow.parties.service.CounterpartyService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/parties/counterparties")
@Tag(name = "Контрагенты", description = "Операции с контрагентами")
class CounterpartyController(
    private val service: CounterpartyService
) {
    @GetMapping("/{id}")
    @Operation(summary = "Получить контрагента по ID")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Контрагент найден")
    @CommonSecurityResponses
    @NotFoundResponse
    @InternalServerErrorResponse
    fun findById(@PathVariable id: Int): CounterpartyDto = service.getById(id)

    @GetMapping
    @Operation(summary = "Поиск контрагентов по фильтру названия и guid (с пагинацией)")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    @ApiResponse(responseCode = "200", description = "Список контрагентов получен")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun findByFilter(
        @RequestParam(required = false) filter: String?,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.ASC) @ParameterObject pageable: Pageable
    ): PagedModel<CounterpartyDto> {
        val page = service.findByFilter(filter, pageable)
        return PagedModel(page)
    }

    @PostMapping
    @Operation(summary = "Создать нового контрагента")
    @PreAuthorize("hasAnyAuthority('SCOPE_create','SCOPE_manager')")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "Контрагент создан")
    @ValidationErrorResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun create(@Valid @RequestBody req: CounterpartyCreateRequest): CounterpartyDto = service.create(req)

    @PutMapping("/{id}")
    @Operation(summary = "Изменить контрагента по заданному ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_update','SCOPE_manager')")
    @ApiResponse(responseCode = "200", description = "Контрагент обновлён")
    @ValidationErrorResponse
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun update(@PathVariable id: Int, @Valid @RequestBody req: CounterpartyUpdateRequest): CounterpartyDto =
        service.update(id, req)

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить контрагента по заданному ID")
    @PreAuthorize("hasAnyAuthority('SCOPE_delete','SCOPE_manager')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Контрагент удалён")
    @NotFoundResponse
    @ConflictResponse
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun delete(@PathVariable id: Int) {
        service.delete(id)
    }
}