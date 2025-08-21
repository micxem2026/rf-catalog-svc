package me.rightsflow.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.OffsetDateTime

@MappedSuperclass
abstract class BaseAudit {
    @Column(name = "CREATED_BY", nullable = false, length = 20)
    var createdBy: String = "system"

    @Column(name = "CREATED_AT", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @Column(name = "UPDATED_BY", length = 20)
    var updatedBy: String? = null

    @Column(name = "UPDATED_AT")
    var updatedAt: OffsetDateTime? = null
}