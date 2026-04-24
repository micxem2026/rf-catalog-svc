package me.rightsflow.acl.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import me.rightsflow.acl.dto.KlfCounterpartyRequest
import me.rightsflow.acl.dto.KlfFeaturePlainRequest
import me.rightsflow.acl.dto.KlfFeatureTreeRequest
import me.rightsflow.acl.dto.KlfOipHierarchyRequest
import me.rightsflow.acl.dto.KlfOipRequest
import me.rightsflow.acl.dto.KlfOrganizationRequest
import me.rightsflow.acl.dto.KlfRightTypeRequest
import me.rightsflow.acl.dto.LovOipTypeRequest
import me.rightsflow.acl.service.AclSyncService
import me.rightsflow.common.config.CommonSecurityResponses
import me.rightsflow.common.config.InternalServerErrorResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/acl")
@Tag(name = "ACL", description = "Операции для работы acl-слоя")
class AclController(
    private val service: AclSyncService
) {

    @PostMapping("/syncKlfRightType")
    @Operation(summary = "Синхронизация таблицы KLF_RIGHT_TYPE")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_RIGHT_TYPE выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfRightType(@Valid @RequestBody req: KlfRightTypeRequest): Int? = service.syncKlfRightType(req)

    @PostMapping("/syncKlfFeaturePlain")
    @Operation(summary = "Синхронизация таблицы KLF_FEATURE_PLAIN")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_FEATURE_PLAIN выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfFeaturePlain(@Valid @RequestBody req: KlfFeaturePlainRequest): Int? = service.syncKlfFeaturePlain(req)

    @PostMapping("/syncKlfFeatureTree")
    @Operation(summary = "Синхронизация таблицы KLF_FEATURE_TREE")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_FEATURE_TREE выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfFeatureTree(@Valid @RequestBody req: KlfFeatureTreeRequest): Int? = service.syncKlfFeatureTree(req)

    @PostMapping("/syncLovOipType")
    @Operation(summary = "Синхронизация таблицы LOV_OIP_TYPE")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация LOV_OIP_TYPE выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncLovOipType(@Valid @RequestBody req: LovOipTypeRequest): Int? = service.syncLovOipType(req)

    @PostMapping("/syncKlfOip")
    @Operation(summary = "Синхронизация таблицы KLF_OIP")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_OIP выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfOip(@Valid @RequestBody req: KlfOipRequest): Int? = service.syncKlfOip(req)

    @PostMapping("/syncKlfOipHierarchy")
    @Operation(summary = "Синхронизация таблицы KLF_OIP_HIERARCHY")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_OIP_HIERARCHY выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfOipHierarchy(@Valid @RequestBody req: KlfOipHierarchyRequest): Int? = service.syncKlfOipHierarchy(req)

    @PostMapping("/syncKlfOrganization")
    @Operation(summary = "Синхронизация таблицы KLF_ORGANIZATION")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_ORGANIZATION выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfOrganization(@Valid @RequestBody req: KlfOrganizationRequest): Int? = service.syncKlfOrganization(req)

    @PostMapping("/syncKlfCounterparty")
    @Operation(summary = "Синхронизация таблицы KLF_COUNTERPARTY")
    @PreAuthorize("hasRole('SERVICE')")
    @ApiResponse(responseCode = "200", description = "Синхронизация KLF_COUNTERPARTY выполнена")
    @CommonSecurityResponses
    @InternalServerErrorResponse
    fun syncKlfCounterparty(@Valid @RequestBody req: KlfCounterpartyRequest): Int? = service.syncKlfCounterparty(req)

}