package me.rightsflow.righttypes.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.EntityNotFoundWithClsException
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
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, FeatureCatToRt::class.java) }.toDto()

    fun listByRightType(rightTypeId: Int): List<FeatureCatToRtResponse> =
        repo.findAllByRightTypeId(rightTypeId).map { it.toDto() }

    @Transactional
    fun create(req: FeatureCatToRtCreateRequest): FeatureCatToRtResponse {
        val now = OffsetDateTime.now()
        val who = sub.currentSub()
        val e = FeatureCatToRt().apply {
            idRightType = req.idRightType
            idFeatureCategory = req.idFeatureCategory
            idDefaultFeature = req.idDefaultFeature
            createdBy = who
            createdAt = now
        }
        repo.saveAndFlush(e)     // зафиксировать INSERT
        em.refresh(e)            // перечитать из БД (теперь подтянутся EAGER/LAZY-прокси)
        return e.toDto()
    }

    @Transactional
    fun update(id: Int, req: FeatureCatToRtUpdateRequest): FeatureCatToRtResponse {
        val e = repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, FeatureCatToRt::class.java) }

        req.idRightType?.let { e.idRightType = it } // non-nullable, null -> не меняем
        req.idFeatureCategory?.let { e.idFeatureCategory = it }
        e.idDefaultFeature = req.idDefaultFeature
        e.updatedBy = sub.currentSub()
        e.updatedAt = OffsetDateTime.now()
        repo.saveAndFlush(e)
        em.refresh(e)
        return e.toDto()
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, FeatureCatToRt::class.java)
        repo.deleteById(id) // FK/unique ошибки -> handler (409)
    }

    private fun FeatureCatToRt.toDto() = FeatureCatToRtResponse(
        id = this.id!!,
        idRightType = this.idRightType,
        idFeatureCategory = this.idFeatureCategory,
        idDefaultFeature = this.idDefaultFeature,
        rightTypeName = this.rightType?.name ?: "",
        featureCategoryName = this.featureCategory?.name ?: "",
        defaultFeatureName = this.defaultFeature?.featurePlain?.name,
        createdBy = this.createdBy,
        createdAt = this.createdAt, //.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime(),
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}