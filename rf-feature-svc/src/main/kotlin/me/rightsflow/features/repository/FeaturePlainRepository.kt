package me.rightsflow.features.repository

import me.rightsflow.features.entity.FeaturePlain
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FeaturePlainRepository : JpaRepository<FeaturePlain, Int> {

    @Query("""
        SELECT fp FROM FeaturePlain fp 
        JOIN FETCH fp.featureCategory 
        WHERE (fp.idFeatureCategory = :categoryId)
        AND (LOWER(fp.name) LIKE LOWER(CONCAT('%', :name, '%')) OR :name IS NULL)
    """)
    fun findByFilters(
        @Param("categoryId") categoryId: Int,
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<FeaturePlain>

    @Query("SELECT fp FROM FeaturePlain fp JOIN FETCH fp.featureCategory WHERE fp.id = :id")
    fun findByIdWithCategory(@Param("id") id: Int): FeaturePlain?
}
