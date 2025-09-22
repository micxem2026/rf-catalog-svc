package me.rightsflow.righttypes.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.righttypes.dto.request.RightTypeCreateRequest
import me.rightsflow.righttypes.dto.request.RightTypeUpdateRequest
import me.rightsflow.righttypes.dto.response.RightTypePlainItem
import me.rightsflow.righttypes.dto.response.RightTypeResponse
import me.rightsflow.righttypes.dto.response.RightTypeTreeNode
import me.rightsflow.righttypes.entity.RightType
import me.rightsflow.righttypes.repository.RightTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RightTypeService(
    private val repo: RightTypeRepository,
    private val sub: SecuritySubjectProvider
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    fun getById(id: Int): RightTypeResponse =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, RightType::class.java) }.toDto()

    fun getTree(treeMode: String): Any =
        when (treeMode.lowercase()) {
            "recursive" -> buildRecursiveTree()
            "plain" -> buildPlainTree()
            else -> throw IllegalArgumentException("tree_mode must be 'plain' or 'recursive'")
        }

    @Transactional
    fun create(req: RightTypeCreateRequest): RightTypeResponse {
        val query = em.createNativeQuery("select ins_klf_right_type(:p_id_parent, :p_name, :p_description, :p_created_by)")
            .setParameter("p_id_parent", req.parentId)
            .setParameter("p_name", req.name)
            .setParameter("p_description", req.description)
            .setParameter("p_created_by", sub.currentSub())
        val newId = query.singleResult as Int
        return getById(newId)
    }

    @Transactional
    fun update(id: Int, req: RightTypeUpdateRequest): RightTypeResponse {
        val query = em.createNativeQuery("select upd_klf_right_type(:p_id, :p_parent, :p_name, :p_description, :p_updated_by)")
            .setParameter("p_id", id)
            .setParameter("p_parent", req.parentId)
            .setParameter("p_name", req.name)
            .setParameter("p_description", req.description)
            .setParameter("p_updated_by", sub.currentSub())
        val oldId = query.singleResult as Int
        return getById(oldId)
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, RightType::class.java)
        repo.deleteById(id) // FK конфликты поймаем handler-ом → 409
    }

    // ==== Tree builders ====
    private fun buildRecursiveTree(): List<RightTypeTreeNode> {
        val all = repo.findAllByOrderByIdAsc()
        val byParent = all.groupBy { it.parentId }
        fun build(parentId: Int?): List<RightTypeTreeNode> =
            byParent[parentId].orEmpty().map {
                RightTypeTreeNode(
                    id = it.id!!,
                    parentId = it.parentId,
                    name = it.name,
                    description = it.description,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt,
                    updatedBy = it.updatedBy,
                    updatedAt = it.updatedAt,
                    children = build(it.id)
                )
            }
        // корнем считаем записи с parentId = null
        return build(null)
    }

    private fun buildPlainTree(): List<RightTypePlainItem> {
        val all = repo.findAllByOrderByIdAsc()
        val byParent = all.groupBy { it.parentId }
        val result = mutableListOf<RightTypePlainItem>()

        fun dfs(parentId: Int?, level: Int) {
            byParent[parentId].orEmpty().sortedBy { it.id }.forEach {
                result += RightTypePlainItem(
                    id = it.id!!,
                    parentId = it.parentId,
                    name = it.name,
                    description = it.description,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt,
                    updatedBy = it.updatedBy,
                    updatedAt = it.updatedAt,
                    level = level
                )
                dfs(it.id, level + 1)
            }
        }
        dfs(null, 1) // уровень корня = 1
        return result
    }

    private fun RightType.toDto() = RightTypeResponse(
        id = this.id!!,
        parentId = this.parentId,
        name = this.name,
        description = this.description,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}