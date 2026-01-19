package me.rightsflow.parties.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.clients.feign.ContractConstraintClient
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.ConstraintException
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.parties.dto.request.CounterpartyCreateRequest
import me.rightsflow.parties.dto.request.CounterpartyUpdateRequest
import me.rightsflow.parties.dto.response.CounterpartyDto
import me.rightsflow.parties.entity.Counterparty
import me.rightsflow.parties.repository.CounterpartyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class CounterpartyService(
    private val repo: CounterpartyRepository,
    private val sub: SecuritySubjectProvider,
    private val constraintClient: ContractConstraintClient,
    @PersistenceContext private val em: EntityManager
) {
    fun getById(id: Int): CounterpartyDto =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Counterparty::class.java) }.toDto()

    fun findByFilter(filter: String?, pageable: Pageable): Page<CounterpartyDto> =
        repo.findByFilter(filter, pageable).map { it.toDto() }

    @Transactional
    fun create(req: CounterpartyCreateRequest): CounterpartyDto {
        val e = Counterparty(
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
    fun update(id: Int, req: CounterpartyUpdateRequest): CounterpartyDto {
        val e = repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Counterparty::class.java) }
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
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, Counterparty::class.java)
        if (!constraintClient.checkCounterpartyConstraint(id)) {
            repo.deleteById(id)
        } else {
            throw ConstraintException(id, Counterparty::class.java)
        }
    }

    private fun Counterparty.toDto() = CounterpartyDto(
        id = this.id!!,
        guid = this.guid,
        code1c = this.code_1c,
        name = this.name,
        idOrgRef = this.idOrgRef,
        nameOrgRef = this.organization?.name,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}