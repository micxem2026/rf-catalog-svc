package me.rightsflow.features.service

import me.rightsflow.features.dto.request.CreateFeatureCategoryRequest
import me.rightsflow.features.dto.request.UpdateFeatureCategoryRequest
import me.rightsflow.features.dto.response.FeatureCategoryResponse
import me.rightsflow.features.entity.FeatureCategory
import me.rightsflow.features.exception.EntityNotFoundException
import me.rightsflow.features.repository.FeatureCategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class FeatureCategoryService(
    private val featureCategoryRepository: FeatureCategoryRepository
) {

    private val log = LoggerFactory.getLogger(FeatureCategoryService::class.java)

    @Transactional(readOnly = true)
    fun findById(id: Int): FeatureCategoryResponse {
        log.debug("Finding feature category by id: $id")
        val category = featureCategoryRepository.findById(id)
            .orElseThrow { EntityNotFoundException(id) }
        return mapToResponse(category)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<FeatureCategoryResponse> {
        if (log.isDebugEnabled)
            log.debug("Finding all feature categories with pagination: $pageable")
        return featureCategoryRepository.findAll(pageable)
            .map { mapToResponse(it) }
    }

    fun create(request: CreateFeatureCategoryRequest, userId: String): FeatureCategoryResponse {
        log.debug("Creating new feature category: ${request.name} by user: $userId")
        val category = FeatureCategory(
            name = request.name,
            createdBy = userId
        )
        val savedCategory = featureCategoryRepository.save(category)
        log.debug("Created feature category with id: ${savedCategory.id}")
        return mapToResponse(savedCategory)
    }

    fun update(id: Int, request: UpdateFeatureCategoryRequest, userId: String): FeatureCategoryResponse {
        log.debug("Updating feature category with id: $id by user: $userId")
        val category = featureCategoryRepository.findById(id)
            .orElseThrow { EntityNotFoundException(id) }

        request.name?.let {
            if (it.isNotBlank()) {
                category.name = it
            }
        }
        category.updatedBy = userId
        category.updatedAt = LocalDateTime.now()

        val updatedCategory = featureCategoryRepository.save(category)
        log.debug("Updated feature category with id: $id")
        return mapToResponse(updatedCategory)
    }

    fun deleteById(id: Int) {
        log.debug("Deleting feature category with id: $id")
        if (!featureCategoryRepository.existsById(id)) {
            throw EntityNotFoundException(id)
        }
        featureCategoryRepository.deleteById(id)
        log.debug("Deleted feature category with id: $id")
    }

    private fun mapToResponse(category: FeatureCategory): FeatureCategoryResponse {
        return FeatureCategoryResponse(
            id = category.id!!,
            name = category.name,
            createdBy = category.createdBy,
            createdAt = category.createdAt,
            updatedBy = category.updatedBy,
            updatedAt = category.updatedAt
        )
    }
}
