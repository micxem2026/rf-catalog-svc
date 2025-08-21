package me.rightsflow.oips.repository

import me.rightsflow.oips.entity.OipType
import org.springframework.data.jpa.repository.JpaRepository

interface OipTypeRepository : JpaRepository<OipType, Int>