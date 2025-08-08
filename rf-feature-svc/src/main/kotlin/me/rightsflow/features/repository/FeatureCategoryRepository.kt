package me.rightsflow.features.repository

import me.rightsflow.features.entity.FeatureCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FeatureCategoryRepository : JpaRepository<FeatureCategory, Int>
