package me.rightsflow.clients.feign

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContractConstraintClientFallback : ContractConstraintClient {

    private val logger = LoggerFactory.getLogger(ContractConstraintClientFallback::class.java)

    override fun checkOipConstraint(id: Int): Boolean {
        logger.warn("Fallback: Failed to check OIP constraint for id=$id. Returning true.")
        return true
    }

    override fun checkRightTypeConstraint(id: Int): Boolean {
        logger.warn("Fallback: Failed to check right type constraint for id=$id. Returning true.")
        return true
    }

    override fun checkFeatureCategoryConstraint(id: Int): Boolean {
        logger.warn("Fallback: Failed to check feature category constraint for id=$id. Returning true.")
        return true
    }

    override fun checkFeatureConstraint(id: Int): Boolean {
        logger.warn("Fallback: Failed to check feature constraint for id=$id. Returning true.")
        return true
    }

    override fun checkCounterpartyConstraint(id: Int): Boolean {
        logger.warn("Fallback: Failed to check counterparty constraint for id=$id. Returning true.")
        return true
    }

    override fun checkOrganizationConstraint(id: Int): Boolean {
        logger.warn("Fallback: Failed to check organization constraint for id=$id. Returning true.")
        return true
    }
}