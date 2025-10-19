package me.rightsflow.clients.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "rf-contract-svc",
    contextId = "catalog-contract-client",
    path = "\${rightsflow.services.rf-contract-svc.prefix:/api/contract/v1}",
    fallback = ContractConstraintClientFallback::class
)
interface ContractConstraintClient {

    @GetMapping("/constraints/oip/{id}")
    fun checkOipConstraint(@PathVariable id: Int): Boolean

    @GetMapping("/constraints/right-type/{id}")
    fun checkRightTypeConstraint(@PathVariable id: Int): Boolean

    @GetMapping("/constraints/feature-category/{id}")
    fun checkFeatureCategoryConstraint(@PathVariable id: Int): Boolean

    @GetMapping("/constraints/feature/{id}")
    fun checkFeatureConstraint(@PathVariable id: Int): Boolean

    @GetMapping("/constraints/counterparty/{id}")
    fun checkCounterpartyConstraint(@PathVariable id: Int): Boolean

    @GetMapping("/constraints/organization/{id}")
    fun checkOrganizationConstraint(@PathVariable id: Int): Boolean
}