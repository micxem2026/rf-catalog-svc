package me.rightsflow.oips.service

import me.rightsflow.common.exception.EntityNotFoundWithClsException
import me.rightsflow.oips.dto.response.OipSuperTypeDto
import me.rightsflow.oips.entity.OipSuperType
import me.rightsflow.oips.repository.OipSuperTypeRepository
import org.springframework.stereotype.Service

@Service
class OipSuperTypeService(
    private val repo: OipSuperTypeRepository
) {
    fun getById(id: Int): OipSuperTypeDto =
        repo.findById(id).orElseThrow { EntityNotFoundWithClsException(id, OipSuperType::class.java) }.toDto()

    fun findAll(): List<OipSuperTypeDto> =
        repo.findAll().sortedBy { it.id }.map { it.toDto() }

    private fun OipSuperType.toDto() = OipSuperTypeDto(
        id = this.id,
        name = this.name
    )
}