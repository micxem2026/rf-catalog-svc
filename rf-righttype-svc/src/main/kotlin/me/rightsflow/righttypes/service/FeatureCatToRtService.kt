package me.rightsflow.righttypes.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.EntityNotFoundException
import me.rightsflow.righttypes.dto.request.FeatureCatToRtCreateRequest
import me.rightsflow.righttypes.dto.request.FeatureCatToRtUpdateRequest
import me.rightsflow.righttypes.dto.response.FeatureCatToRtResponse
import me.rightsflow.righttypes.entity.FeatureCatToRt
import me.rightsflow.righttypes.repository.FeatureCatToRtRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class FeatureCatToRtService(
    private val repo: FeatureCatToRtRepository,
    private val sub: SecuritySubjectProvider,
    @PersistenceContext private val em: EntityManager
) {

    private val log = LoggerFactory.getLogger(FeatureCatToRtService::class.java)

    fun getById(id: Int): FeatureCatToRtResponse =
        repo.findById(id).orElseThrow { EntityNotFoundException(id) }.toDto()

    fun listByRightType(rightTypeId: Int): List<FeatureCatToRtResponse> =
        repo.findAllByRightTypeId(rightTypeId).map { it.toDto() }

    @Transactional
    fun create(req: FeatureCatToRtCreateRequest): FeatureCatToRtResponse {
        val now = OffsetDateTime.now()
        val who = sub.currentSub()
        val e = FeatureCatToRt().apply {
            rightTypeId = req.rightTypeId
            featureCategoryId = req.featureCategoryId
            defaultFeatureId = req.defaultFeatureId
            createdBy = who
            createdAt = now
        }
        repo.saveAndFlush(e)     // зафиксировать INSERT
        em.refresh(e)            // перечитать из БД (теперь подтянутся EAGER/LAZY-прокси)
        return e.toDto()
    }

    @Transactional
    fun update(id: Int, req: FeatureCatToRtUpdateRequest): FeatureCatToRtResponse {
        val e = repo.findById(id).orElseThrow { EntityNotFoundException(id) }

        req.rightTypeId?.let { e.rightTypeId = it } // non-nullable, null -> не меняем
        req.featureCategoryId?.let { e.featureCategoryId = it }
        e.defaultFeatureId = req.defaultFeatureId
        e.updatedBy = sub.currentSub()
        e.updatedAt = OffsetDateTime.now()
        repo.saveAndFlush(e)
        em.refresh(e)
        return e.toDto()
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundException(id)
        repo.deleteById(id) // FK/unique ошибки -> handler (409)
    }

    private fun FeatureCatToRt.toDto() = FeatureCatToRtResponse(
        id = this.id!!,
        rightTypeId = this.rightTypeId,
        featureCategoryId = this.featureCategoryId,
        defaultFeatureId = this.defaultFeatureId,
        rightTypeName = this.rightType?.name ?: "",
        featureCategoryName = this.featureCategory?.name ?: "",
        defaultFeatureName = this.defaultFeature?.featurePlain?.name,
        createdBy = this.createdBy,
        createdAt = this.createdAt, //.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime(),
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}