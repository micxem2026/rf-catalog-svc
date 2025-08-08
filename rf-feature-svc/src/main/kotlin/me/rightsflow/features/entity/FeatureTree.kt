package me.rightsflow.features.entity

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "klf_feature_tree")
data class FeatureTree(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tree_seq")
    @SequenceGenerator(name = "tree_seq", sequenceName = "klf_feature_tree_id_seq", allocationSize = 1)
    val id: Int? = null,

    @Column(name = "id_parent")
    var idParent: Int? = null,

    @Column(name = "id_feature_category", nullable = false)
    val idFeatureCategory: Int,

    @Column(name = "id_feature_plain", nullable = false)
    var idFeaturePlain: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parent", insertable = false, updatable = false)
    val parent: FeatureTree? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_feature_category", insertable = false, updatable = false)
    val featureCategory: FeatureCategory? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_feature_plain", insertable = false, updatable = false)
    val featurePlain: FeaturePlain? = null,

    //@JdbcTypeCode(SqlTypes.OTHER)
    @Type(PostgreSQLRangeType::class)
    @Column(name = "validity_period", columnDefinition = "daterange", nullable = false)
    var validityPeriod: Range<LocalDate>,

    @Column(name = "created_by", nullable = false, length = 20)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_by", length = 20)
    var updatedBy: String? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
