package me.rightsflow.righttypes.repository

import me.rightsflow.righttypes.entity.FeatureCatToRt
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface FeatureCatToRtRepository : JpaRepository<FeatureCatToRt, Int> {

    fun findAllByRightTypeId(rightTypeId: Int): List<FeatureCatToRt>

    @EntityGraph(attributePaths = ["rightType", "featureCategory", "defaultFeature"])
    fun findWithRelationsById(id: Int): Optional<FeatureCatToRt>
}