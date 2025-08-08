package me.rightsflow.features.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "klf_feature_plain")
data class FeaturePlain(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plain_seq")
    @SequenceGenerator(name = "plain_seq", sequenceName = "klf_feature_plain_id_seq", allocationSize = 1)
    val id: Int? = null,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "id_feature_category", nullable = false)
    var idFeatureCategory: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_feature_category", insertable = false, updatable = false)
    val featureCategory: FeatureCategory? = null,

    @Column(name = "created_by", nullable = false, length = 20)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_by", length = 20)
    var updatedBy: String? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
