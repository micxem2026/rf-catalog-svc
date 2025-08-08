package me.rightsflow.features.service

import me.rightsflow.features.dto.request.CreateFeaturePlainRequest
import me.rightsflow.features.dto.request.UpdateFeaturePlainRequest
import me.rightsflow.features.dto.response.FeaturePlainResponse
import me.rightsflow.features.entity.FeaturePlain
import me.rightsflow.features.exception.EntityNotFoundException
import me.rightsflow.features.repository.FeatureCategoryRepository
import me.rightsflow.features.repository.FeaturePlainRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class FeaturePlainService(
    private val featurePlainRepository: FeaturePlainRepository,
    private val featureCategoryRepository: FeatureCategoryRepository
) {

    private val log = LoggerFactory.getLogger(FeaturePlainService::class.java)

    @Transactional(readOnly = true)
    fun findById(id: Int): FeaturePlainResponse {
        log.debug("Finding feature plain by id: $id")
        val featurePlain = featurePlainRepository.findByIdWithCategory(id)
            ?: throw EntityNotFoundException(id)
        return mapToResponse(featurePlain)
    }

    @Transactional(readOnly = true)
    fun findAll(
        categoryId: Int,
        name: String?,
        pageable: Pageable
    ): Page<FeaturePlainResponse> {
        if (log.isDebugEnabled)
            log.debug("Finding feature plains with filters - categoryId: $categoryId, name: $name, pageable: $pageable")
        return featurePlainRepository.findByFilters(categoryId, name, pageable)
            .map { mapToResponse(it) }
    }

    fun create(request: CreateFeaturePlainRequest, userId: String): FeaturePlainResponse {
        log.debug("Creating new feature plain: ${request.name} by user: $userId")

        // Проверяем существование категории
        if (!featureCategoryRepository.existsById(request.idFeatureCategory)) {
            throw EntityNotFoundException(request.idFeatureCategory)
        }

        val featurePlain = FeaturePlain(
            name = request.name,
            idFeatureCategory = request.idFeatureCategory,
            createdBy = userId
        )

        val savedFeaturePlain = featurePlainRepository.save(featurePlain)
        log.debug("Created feature plain with id: ${savedFeaturePlain.id}")

        return findById(savedFeaturePlain.id!!)
    }

    fun update(id: Int, request: UpdateFeaturePlainRequest, userId: String): FeaturePlainResponse {
        log.debug("Updating feature plain with id: $id by user: $userId")
        val featurePlain = featurePlainRepository.findById(id)
            .orElseThrow { EntityNotFoundException(id) }

        request.name?.let {
            if (it.isNotBlank()) {
                featurePlain.name = it
            }
        }

        request.idFeatureCategory?.let { categoryId ->
            // Проверяем существование категории
            if (!featureCategoryRepository.existsById(categoryId)) {
                throw EntityNotFoundException(categoryId)
            }
            featurePlain.idFeatureCategory = categoryId
        }

        featurePlain.updatedBy = userId
        featurePlain.updatedAt = LocalDateTime.now()

        featurePlainRepository.save(featurePlain)
        log.debug("Updated feature plain with id: $id")

        return findById(id)
    }

    fun deleteById(id: Int) {
        log.debug("Deleting feature plain with id: $id")
        if (!featurePlainRepository.existsById(id)) {
            throw EntityNotFoundException(id)
        }
        featurePlainRepository.deleteById(id)
        log.debug("Deleted feature plain with id: $id")
    }

    private fun mapToResponse(featurePlain: FeaturePlain): FeaturePlainResponse {
        return FeaturePlainResponse(
            id = featurePlain.id!!,
            name = featurePlain.name,
            idFeatureCategory = featurePlain.idFeatureCategory,
            categoryName = featurePlain.featureCategory?.name ?: "",
            createdBy = featurePlain.createdBy,
            createdAt = featurePlain.createdAt,
            updatedBy = featurePlain.updatedBy,
            updatedAt = featurePlain.updatedAt
        )
    }
}
