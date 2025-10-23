package me.rightsflow.oips.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.clients.feign.ContractConstraintClient
import me.rightsflow.common.config.SecuritySubjectProvider
import me.rightsflow.common.exception.ConstraintException
import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.common.util.DurationUtil
import me.rightsflow.oips.dto.request.OipCreateRequest
import me.rightsflow.oips.dto.request.OipUpdateRequest
import me.rightsflow.oips.dto.response.OipDto
import me.rightsflow.oips.entity.Oip
import me.rightsflow.oips.repository.OipRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class OipService(
    private val repo: OipRepository,
    private val subProvider: SecuritySubjectProvider,
    private val contractConstraintClient: ContractConstraintClient,
    @PersistenceContext private val em: EntityManager
) {
    fun getById(id: Int): OipDto =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Oip::class.java) }.toDto()

    fun findByFilter(
        idOipSuperType: Int?,
        idOipType: Int?,
        nodeType: Oip.NodeType?,
        filter: String?,
        pageable: Pageable
    ): Page<OipDto> =
        when (nodeType) {
            null ->  repo.findByFilter(idOipSuperType, idOipType, null, null,
                                       filter, pageable).map { it.toDto() }
            Oip.NodeType.ROOT -> repo.findByFilter(idOipSuperType, idOipType, true, false,
                                                    filter, pageable).map { it.toDto() }
            Oip.NodeType.LEAF -> repo.findByFilter(idOipSuperType, idOipType, false, true,
                                                   filter, pageable).map { it.toDto() }
            Oip.NodeType.BRANCH -> repo.findByFilter(idOipSuperType, idOipType, true, true,
                                                     filter, pageable).map { it.toDto() }
            Oip.NodeType.ISOLATED -> repo.findByFilter(idOipSuperType, idOipType, false, false,
                                                       filter, pageable).map { it.toDto() }
        }

    @Transactional
    fun create(req: OipCreateRequest): OipDto {
        val e = Oip(
            guid = req.guid,
            idOipSuperType = requireNotNull(req.idOipSuperType) { "idOipSuperType is required" },
            idOipType = requireNotNull(req.idOipType) { "idOipType is required" },
            name = requireNotNull(req.name) { "name is required" },
            partNum = req.partNum,
            partCount = req.partCount,
            duration = DurationUtil.fromStringHHmmss(req.duration),
            description = req.description
        ).apply {
            createdBy = subProvider.currentSub()
            createdAt = OffsetDateTime.now()
        }

        repo.saveAndFlush(e)
        em.refresh(e)
        return e.toDto()
    }

    @Transactional
    fun update(id: Int, req: OipUpdateRequest): OipDto {
        val e = repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, Oip::class.java) }
        // NOT NULL поля: если пришло null — НЕ меняем
        req.idOipSuperType?.let { e.idOipSuperType = it }
        req.idOipType?.let { e.idOipType = it }
        req.name?.let { e.name = it }
        req.partNum?.let { e.partNum = it }
        req.partCount?.let { e.partCount = it }
        // GUID nullable — меняем если прислали
        if (req.guid != null) e.guid = req.guid
        // DURATION nullable — записываем что пришло (включая null)
        e.duration = DurationUtil.fromStringHHmmss(req.duration)
        // DESCRIPTION nullable — записываем что пришло (включая null)
        e.description = req.description
        e.updatedBy = subProvider.currentSub()
        e.updatedAt = OffsetDateTime.now()
        repo.saveAndFlush(e)
        em.refresh(e)
        return e.toDto()
    }

    @Transactional
    fun delete(id: Int) {
        if (!repo.existsById(id)) throw EntityNotFoundWithClsException(id, Oip::class.java)
        if (!contractConstraintClient.checkOipConstraint(id)) {
            repo.deleteById(id)
        } else {
            throw ConstraintException(id, Oip::class.java)
        }
    }

    private fun Oip.toDto() = OipDto(
        id = this.id!!,
        guid = this.guid,
        idOipSuperType = this.idOipSuperType,
        idOipType = this.idOipType,
        name = this.name,
        partNum = this.partNum,
        partCount = this.partCount,
        duration = DurationUtil.toStringHHmmss(this.duration),
        oipSuperTypeName = this.oipSuperType?.name ?: "",
        oipTypeName = this.oipType?.name ?: "",
        description = this.description,
        hasParent = this.hasParent,
        hasChildren = this.hasChildren,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedBy = this.updatedBy,
        updatedAt = this.updatedAt
    )
}