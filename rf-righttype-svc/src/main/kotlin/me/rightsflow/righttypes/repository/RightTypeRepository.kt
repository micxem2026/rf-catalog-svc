package me.rightsflow.righttypes.repository

import me.rightsflow.righttypes.entity.RightType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RightTypeRepository : JpaRepository<RightType, Int> {

    fun findAllByOrderByIdAsc(): List<RightType>

    @Query("select r from RightType r where r.idParent is null order by r.id asc")
    fun findRoots(): List<RightType>

    fun findByParentIdOrderByIdAsc(parentId: Int?): List<RightType>
}