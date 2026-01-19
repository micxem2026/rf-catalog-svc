package me.rightsflow.parties.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.clients.feign.ContractConstraintClient
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.ConstraintException
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.parties.dto.request.OrganizationCreateRequest
import me.rightsflow.parties.dto.request.OrganizationUpdateRequest
import me.rightsflow.parties.dto.response.OrganizationDto
import me.rightsflow.parties.entity.Organization
import me.rightsflow.parties.repository.OrganizationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class OrganizationService(
    private val repo: OrganizationRepository,
    private val sub: SecuritySubjectProvider,
    private val constraintClient: ContractConstraintClient,
    @PersistenceContext private val em: EntityManager
) {
    fun getById(id: Int): OrganizationDto =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Organization::class.java) }.toDto()

    fun findByFilter(filter: String?, pageable: Pageable): Page<OrganizationDto> =
        repo.findByFilter(filter, pageable).map { it.toDto() }

    @Transactional
    fun create(req: OrganizationCreateRequest): OrganizationDto {
        val e = Organization(
            guid = req.guid,
            code_1c = req.code1c?.uppercase(),
            name = requireNotNull(req.name) { "name is required" },
        ).apply {
            createdBy = sub.currentSub()
            createdAt = OffsetDateTime.now()
        }

        repo.saveAndFlush(e)
        em.refresh(e)
        return e.toDto()
    }

    @Transactional
    fun update(id: Int, req: OrganizationUpdateRequest): OrganizationDto {
        val e = repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Organization::class.java) }
        // NOT NULL поля: если пришло null — НЕ меняем
        req.name?.let { e.name = it }
        // GUID nullable — меняем если прислали
        if (req.guid != null) e.guid = req.guid
        if (req.code1c != null) e.code_1c = req.code1c.uppercase()
        e.updatedBy = sub.currentSub()
        e.updatedAt = OffsetDateTime.now()
        repo.saveAndFlush(e)
        em.refresh(e)
        return e.toDto()
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, Organization::class.java)
        if (!constraintClient.checkOrganizationConstraint(id)) {
            repo.deleteById(id)
        } else {
            throw ConstraintException(id, Organization::class.java)
        }
    }

    private fun Organization.toDto() = OrganizationDto(
        id = this.id!!,
        guid = this.guid,
        code1c = this.code_1c,
        name = this.name,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}