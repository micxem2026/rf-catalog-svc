package me.rightsflow.features.service

import me.rightsflow.clients.feign.ContractConstraintClient
import me.rightsflow.common.exception.ConstraintException
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.features.dto.request.CreateFeatureTreeRequest
import me.rightsflow.features.dto.request.UpdateFeatureTreeRequest
import me.rightsflow.features.dto.response.*
import me.rightsflow.features.entity.FeaturePlain
import me.rightsflow.features.entity.FeatureTree
import me.rightsflow.features.repository.FeaturePlainRepository
import me.rightsflow.features.repository.FeatureTreeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FeatureTreeService(
    private val featureTreeRepository: FeatureTreeRepository,
    private val featurePlainRepository: FeaturePlainRepository,
    private val contractConstraintClient: ContractConstraintClient
) {

    private val log = LoggerFactory.getLogger(FeatureTreeService::class.java)

    @Transactional(readOnly = true)
    fun findById(id: Int): FeatureTreeProjection {
        log.debug("Finding feature tree by id: $id")
        val featureTree = featureTreeRepository.findByIdWithRelations(id)
            ?: throw EntityNotFoundWithClsException(id, FeatureTree::class.java)
        return featureTree
    }

    @Transactional(readOnly = true)
    fun findTreeByCategory(categoryId: Int, treeMode: String): Any {
        log.debug("Finding feature tree by category: $categoryId, mode: $treeMode")

        return when (treeMode.lowercase()) {
            "recursive" -> {
                val roots = featureTreeRepository.findRootsByCategory(categoryId)
                roots.map { buildRecursiveTree(it) }
            }

            "plain" -> {
                val allNodes = featureTreeRepository.findAllByCategory(categoryId)
                allNodes.map { mapToPlainResponse(it, calculateLevel(it, allNodes)) }
            }

            else -> throw IllegalArgumentException("Invalid tree_mode. Expected 'recursive' or 'plain'")
        }
    }

    fun create(request: CreateFeatureTreeRequest, userId: String): FeatureTreeProjection {
        log.debug("Creating new feature tree by user: $userId")

        // Проверяем существование простой характеристики
        if (!featurePlainRepository.existsById(request.idFeaturePlain)) {
            throw EntityNotFoundWithClsException(request.idFeaturePlain, FeaturePlain::class.java)
        }

        // Проверяем существование родителя, если указан
        request.idParent?.let { parentId ->
            if (!featureTreeRepository.existsById(parentId)) {
                throw EntityNotFoundWithClsException(parentId, FeatureTree::class.java)
            }
        }

        val generatedId = featureTreeRepository.addFeatureTree(
            idParent = request.idParent,
            idFeaturePlain = request.idFeaturePlain,
            userId = userId,
            beginDate = request.beginDate,
            endDate = request.endDate
        )
        log.debug("Created feature tree with id: $generatedId")
        return findById(generatedId)

    }

    fun update(id: Int, request: UpdateFeatureTreeRequest, userId: String): FeatureTreeProjection {
        log.debug("Updating feature tree with id: $id by user: $userId")

        // Проверяем существование записи
        if (!featureTreeRepository.existsById(id)) {
            throw EntityNotFoundWithClsException(id, FeatureTree::class.java)
        }

        val currentTree = featureTreeRepository.findByIdWithRelations(id)!!

        // Определяем значения для обновления
        val newIdParent = request.idParent ?: currentTree.idParent
        val newIdFeaturePlain = request.idFeaturePlain ?: currentTree.idFeaturePlain

        // Проверяем существование простой характеристики, если она изменяется
        request.idFeaturePlain?.let { plainId ->
            if (!featurePlainRepository.existsById(plainId)) {
                throw EntityNotFoundWithClsException(plainId, FeaturePlain::class.java)
            }
        }

        // Проверяем существование родителя, если он изменяется
        request.idParent?.let { parentId ->
            if (!featureTreeRepository.existsById(parentId)) {
                throw EntityNotFoundWithClsException(parentId, FeatureTree::class.java)
            }
        }

        featureTreeRepository.updateFeatureTree(
            id = id,
            idParent = newIdParent,
            idFeaturePlain = newIdFeaturePlain,
            userId = userId,
            beginDate = request.beginDate,
            endDate = request.endDate
        )
        log.debug("Updated feature tree with id: $id")
        return findById(id)

    }

    fun deleteById(id: Int) {
        log.debug("Deleting feature tree with id: $id")
        if (!featureTreeRepository.existsById(id)) {
            throw EntityNotFoundWithClsException(id, FeatureTree::class.java)
        }
        if (!contractConstraintClient.checkFeatureConstraint(id)) {
            featureTreeRepository.deleteById(id)
            log.debug("Deleted feature tree with id: $id")
        } else {
            throw ConstraintException(id, FeatureTree::class.java)
        }
    }

    private fun buildRecursiveTree(node: FeatureTreeRecursiveProjection): FeatureTreeRecursiveProjection {
        val children = featureTreeRepository.findByParentIdWithRelations(node.id)
            .map { buildRecursiveTree(it) }

        return FeatureTreeRecursiveResponse(
            node.id,
            node.idParent,
            node.idFeatureCategory,
            node.categoryName,
            node.idFeaturePlain,
            node.plainName,
            node.beginDate,
            node.endDate,
            node.createdBy,
            node.createdAt,
            node.updatedBy,
            node.updatedAt,
            children
        )
    }

    private fun calculateLevel(node: FeatureTreePlainProjection, allNodes: List<FeatureTreePlainProjection>): Int {
        if (node.idParent == null) return 1

        val parent = allNodes.find { it.id == node.idParent }
        return if (parent != null) {
            calculateLevel(parent, allNodes) + 1
        } else {
            1
        }
    }

    private fun mapToPlainResponse(node: FeatureTreePlainProjection, level: Int): FeatureTreePlainProjection {
        return FeatureTreePlainResponse(
            node.id,
            node.idParent,
            node.idFeatureCategory,
            node.categoryName,
            node.idFeaturePlain,
            node.plainName,
            node.beginDate,
            node.endDate,
            node.createdBy,
            node.createdAt,
            node.updatedBy,
            node.updatedAt,
            level
        )
    }
}
