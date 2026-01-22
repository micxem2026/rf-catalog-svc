package me.rightsflow.features.repository

import me.rightsflow.features.dto.response.FeatureTreePlainProjection
import me.rightsflow.features.dto.response.FeatureTreeProjection
import me.rightsflow.features.dto.response.FeatureTreeRecursiveProjection
import me.rightsflow.features.entity.FeatureTree
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface FeatureTreeRepository : JpaRepository<FeatureTree, Int> {

    @Query(
        """
    SELECT ft.id, ft.id_parent as idParent, ft.id_feature_category as idFeatureCategory, fc.name as categoryName, 
           ft.id_feature_plain as idFeaturePlain, fp.name, lower(ft.validity_period) as beginDate, upper(ft.validity_period) as endDate, 
           ft.created_by as createdBy, ft.created_at as createdAt, ft.updated_by as updatedBy, ft.updated_at as updatedAt 
    FROM klf_feature_tree ft 
    LEFT JOIN klf_feature_category fc ON ft.id_feature_category = fc.id
    LEFT JOIN klf_feature_plain fp ON ft.id_feature_plain = fp.id
    WHERE ft.id = :id
    """, nativeQuery = true
    )
    fun findByIdWithRelations(@Param("id") id: Int): FeatureTreeProjection?

    @Query(
        """
    SELECT ft.id, ft.id_parent as idParent, ft.id_feature_category as idFeatureCategory, fc.name as categoryName, 
           ft.id_feature_plain as idFeaturePlain, fp.name, lower(ft.validity_period) as beginDate, upper(ft.validity_period) as endDate, 
           ft.created_by as createdBy, ft.created_at as createdAt, ft.updated_by as updatedBy, ft.updated_at as updatedAt 
    FROM klf_feature_tree ft 
    LEFT JOIN klf_feature_category fc ON ft.id_feature_category = fc.id
    LEFT JOIN klf_feature_plain fp ON ft.id_feature_plain = fp.id
    WHERE ft.id_feature_category = :categoryId 
    AND ft.id_parent IS NULL
    AND CURRENT_DATE <@ ft.validity_period
    ORDER BY ft.id
    """, nativeQuery = true
    )
    fun findRootsByCategory(@Param("categoryId") categoryId: Int): List<FeatureTreeRecursiveProjection>

    @Query(
        """
    SELECT ft.id, ft.id_parent as idParent, ft.id_feature_category as idFeatureCategory, fc.name as categoryName, 
           ft.id_feature_plain as idFeaturePlain, fp.name, lower(ft.validity_period) as beginDate, upper(ft.validity_period) as endDate, 
           ft.created_by as createdBy, ft.created_at as createdAt, ft.updated_by as updatedBy, ft.updated_at as updatedAt, 0 as level 
    FROM klf_feature_tree ft 
    LEFT JOIN klf_feature_category fc ON ft.id_feature_category = fc.id
    LEFT JOIN klf_feature_plain fp ON ft.id_feature_plain = fp.id
    WHERE ft.id_feature_category = :categoryId 
    AND CURRENT_DATE <@ ft.validity_period
    ORDER BY ft.id
    """, nativeQuery = true
    )
    fun findAllByCategory(@Param("categoryId") categoryId: Int): List<FeatureTreePlainProjection>

    @Query(
        """
    SELECT ft.id, ft.id_parent as idParent, ft.id_feature_category as idFeatureCategory, fc.name as categoryName, 
           ft.id_feature_plain as idFeaturePlain, fp.name, lower(ft.validity_period) as beginDate, upper(ft.validity_period) as endDate, 
           ft.created_by as createdBy, ft.created_at as createdAt, ft.updated_by as updatedBy, ft.updated_at as updatedAt 
    FROM klf_feature_tree ft 
    LEFT JOIN klf_feature_category fc ON ft.id_feature_category = fc.id
    LEFT JOIN klf_feature_plain fp ON ft.id_feature_plain = fp.id
    WHERE ft.id_parent = :parentId 
    AND CURRENT_DATE <@ ft.validity_period
    ORDER BY ft.id
    """, nativeQuery = true
    )
    fun findByParentIdWithRelations(@Param("parentId") parentId: Int): List<FeatureTreeRecursiveProjection>

    @Query(
        value = "SELECT ins_klf_feature_tree(:p_id_parent, :p_id_feature_plain, :p_created_by, :p_beg_date, :p_end_date)",
        nativeQuery = true
    )
    fun addFeatureTree(
        @Param("p_id_parent") idParent: Int?,
        @Param("p_id_feature_plain") idFeaturePlain: Int,
        @Param("p_created_by") userId: String,
        @Param("p_beg_date") beginDate: LocalDate?,
        @Param("p_end_date") endDate: LocalDate?
    ): Int

    @Query(
        value = "SELECT upd_klf_feature_tree(:p_id, :p_id_parent, :p_id_feature_plain, :p_updated_by, :p_beg_date, :p_end_date)",
        nativeQuery = true
    )
    fun updateFeatureTree(
        @Param("p_id") id: Int,
        @Param("p_id_parent") idParent: Int?,
        @Param("p_id_feature_plain") idFeaturePlain: Int,
        @Param("p_updated_by") userId: String,
        @Param("p_beg_date") beginDate: LocalDate?,
        @Param("p_end_date") endDate: LocalDate?
    ): Int

    @Modifying
    @Query("DELETE FROM FeatureTree ft WHERE ft.id = :id", nativeQuery = false)
    fun deleteByIdWithoutLoad(@Param("id") id: Int): Int
}
