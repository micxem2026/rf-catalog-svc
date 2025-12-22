package me.rightsflow.oips.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.oips.dto.request.OipHierarchyCreateRequest
import me.rightsflow.oips.dto.response.OipHierarchyDto
import me.rightsflow.oips.entity.OipHierarchy
import me.rightsflow.oips.repository.OipHierarchyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OipHierarchyService(
    private val repo: OipHierarchyRepository,
    private val sub: SecuritySubjectProvider
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    fun getById(id: Int): OipHierarchyDto =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, OipHierarchy::class.java) }.toDto()

    fun findChildrenByParent(parentId: Int, pageable: Pageable): Page<OipHierarchyDto> =
        repo.findChildrenSortedByPartNum(parentId, pageable).map { it.toDto() }

    fun findParentsByOip(idOip: Int, pageable: Pageable): Page<OipHierarchyDto> =
        repo.findByIdOipOrderByIdAsc(idOip, pageable).map { it.toDto() }

    @Transactional
    fun create(req: OipHierarchyCreateRequest): OipHierarchyDto {
        val query = em.createNativeQuery("select ins_klf_oip_hierarchy(:p_id_parent, :p_id_oip, :p_created_by)")
            .setParameter("p_id_parent", req.idParent)
            .setParameter("p_id_oip", req.idOip)
            .setParameter("p_created_by", sub.currentSub())
        val newId = query.singleResult as Int
        return getById(newId)
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, OipHierarchy::class.java)
        repo.deleteById(id) // FK конфликты поймаем handler-ом → 409
    }

    private fun OipHierarchy.toDto() = OipHierarchyDto(
        id = this.id,
        idParent = this.idParent,
        idOip = this.idOip,
        parentName = parent?.name ?: "",
        name = oip?.name ?: "",
        hasParent = oip?.hasParent ?: false,
        hasChildren = oip?.hasChildren ?: false,
        childrenCount = oip?.childrenCount ?: 0,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}