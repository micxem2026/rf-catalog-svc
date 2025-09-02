package me.rightsflow.oips.service

import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.oips.dto.response.OipTypeDto
import me.rightsflow.oips.entity.OipType
import me.rightsflow.oips.repository.OipTypeRepository
import org.springframework.stereotype.Service

@Service
class OipTypeService(
    private val repo: OipTypeRepository
) {
    fun getById(id: Int): OipTypeDto =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, OipType::class.java) }.toDto()

    fun findAll(): List<OipTypeDto> =
        repo.findAll().sortedBy { it.id }.map { it.toDto() }

    private fun OipType.toDto() = OipTypeDto(
        id = this.id,
        idOipSuperType = this.idOipSuperType,
        name = this.name,
        superTypeName = this.oipSuperType?.name ?: ""
    )
}