package me.rightsflow.acl.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.acl.dto.KlfCounterpartyRequest
import me.rightsflow.acl.dto.KlfFeaturePlainRequest
import me.rightsflow.acl.dto.KlfFeatureTreeRequest
import me.rightsflow.acl.dto.KlfOipHierarchyRequest
import me.rightsflow.acl.dto.KlfOipRequest
import me.rightsflow.acl.dto.KlfOrganizationRequest
import me.rightsflow.acl.dto.KlfRightTypeRequest
import me.rightsflow.acl.dto.LovOipTypeRequest
import me.rightsflow.clients.feign.ContractConstraintClient
import me.rightsflow.common.exception.ConstraintException
import me.rightsflow.features.entity.FeatureTree
import me.rightsflow.oips.entity.Oip
import me.rightsflow.parties.entity.Counterparty
import me.rightsflow.parties.entity.Organization
import me.rightsflow.righttypes.entity.RightType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AclSyncService(
    @PersistenceContext private val entityManager: EntityManager,
    private val contractConstraintClient: ContractConstraintClient
) {

    private val log = LoggerFactory.getLogger(AclSyncService::class.java)

    @Transactional
    fun syncKlfRightType(
        pod: KlfRightTypeRequest
    ): Int? {

        if (pod.dropFlag && contractConstraintClient.checkRightTypeConstraint(pod.id)) {
            throw ConstraintException(pod.id, RightType::class.java)
        }

        val sql = "SELECT * FROM pkg_acl.sync_klf_right_type(:pId, :pIdParent, :pName, :pDescription, :pIdRightGroup, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pIdParent", pod.idParent)
            .setParameter("pName", pod.name)
            .setParameter("pDescription", pod.description)
            .setParameter("pIdRightGroup", pod.idRightGroup)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncKlfFeaturePlain(
        pod: KlfFeaturePlainRequest
    ): Int? {

        val sql = "SELECT * FROM pkg_acl.sync_klf_feature_plain(:pId, :pIdFeatureCategory, :pName, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pIdFeatureCategory", pod.idFeatureCategory)
            .setParameter("pName", pod.name)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncKlfFeatureTree(
        pod: KlfFeatureTreeRequest
    ): Int? {

        if (pod.dropFlag && contractConstraintClient.checkFeatureConstraint(pod.id)) {
            throw ConstraintException(pod.id, FeatureTree::class.java)
        }

        val sql = "SELECT * FROM pkg_acl.sync_klf_feature_tree(:pId, :pIdParent, :pIdFeatureCategory, :pIdFeaturePlain, :pBegDate, :pEndDate, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pIdParent", pod.idParent)
            .setParameter("pIdFeatureCategory", pod.idFeatureCategory)
            .setParameter("pIdFeaturePlain", pod.idFeaturePlain)
            .setParameter("pBegDate", pod.begDate)
            .setParameter("pEndDate", pod.endDate)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncLovOipType(
        pod: LovOipTypeRequest
    ): Int? {

        val sql = "SELECT * FROM pkg_acl.sync_lov_oip_type(:pId, :pIdOipSuperType, :pName, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pIdOipSuperType", pod.idOipSuperType)
            .setParameter("pName", pod.name)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncKlfOip(
        pod: KlfOipRequest
    ): Int? {

        if (pod.dropFlag && contractConstraintClient.checkOipConstraint(pod.id)) {
            throw ConstraintException(pod.id, Oip::class.java)
        }

        val sql = "SELECT * FROM pkg_acl.sync_klf_oip(:pId, :pGuid, :pIdOipSuperType, :pIdOipType, :pName, :pNativeName, :pFullName, :pReleaseYear, :pPartNum, :pPartCount, :pDuration, :pDescription, :pHasChildren, :pHasParent, :pChildrenCount, :pRootId, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pGuid", pod.guid)
            .setParameter("pIdOipSuperType", pod.idOipSuperType)
            .setParameter("pIdOipType", pod.idOipType)
            .setParameter("pName", pod.name)
            .setParameter("pNativeName", pod.nativeName)
            .setParameter("pFullName", pod.fullName)
            .setParameter("pReleaseYear", pod.releaseYear)
            .setParameter("pPartNum", pod.partNum)
            .setParameter("pPartCount", pod.partCount)
            .setParameter("pDuration", pod.duration)
            .setParameter("pDescription", pod.description)
            .setParameter("pHasChildren", pod.hasChildren)
            .setParameter("pHasParent", pod.hasParent)
            .setParameter("pChildrenCount", pod.childrenCount)
            .setParameter("pRootId", pod.rootId)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncKlfOipHierarchy(
        pod: KlfOipHierarchyRequest
    ): Int? {

        val sql = "SELECT * FROM pkg_acl.sync_klf_oip_hierarchy(:pId, :pIdParent, :pIdOip, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pIdParent", pod.idParent)
            .setParameter("pIdOip", pod.idOip)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncKlfOrganization(
        pod: KlfOrganizationRequest
    ): Int? {

        if (pod.dropFlag && contractConstraintClient.checkOrganizationConstraint(pod.id)) {
            throw ConstraintException(pod.id, Organization::class.java)
        }

        val sql = "SELECT * FROM pkg_acl.sync_klf_organization(:pId, :pName, :pCode1c, :pCountry, :pAddress, :pTin, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pName", pod.name)
            .setParameter("pCode1c", pod.code1c)
            .setParameter("pCountry", pod.country)
            .setParameter("pAddress", pod.address)
            .setParameter("pTin", pod.tin)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }

    @Transactional
    fun syncKlfCounterparty(
        pod: KlfCounterpartyRequest
    ): Int? {

        if (pod.dropFlag && contractConstraintClient.checkCounterpartyConstraint(pod.id)) {
            throw ConstraintException(pod.id, Counterparty::class.java)
        }

        val sql = "SELECT * FROM pkg_acl.sync_klf_counterparty(:pId, :pName, :pCode1c, :pCountry, :pAddress, :pTin, :pIdOrgRef, :pDropFlag)"
        val result = entityManager.createNativeQuery(sql)
            .setParameter("pId", pod.id)
            .setParameter("pName", pod.name)
            .setParameter("pCode1c", pod.code1c)
            .setParameter("pCountry", pod.country)
            .setParameter("pAddress", pod.address)
            .setParameter("pTin", pod.tin)
            .setParameter("pIdOrgRef", pod.idOrgRef)
            .setParameter("pDropFlag", pod.dropFlag)
            .singleResult

        return when (result) {
            is Number -> result.toInt()
            else -> null
        }
    }


}