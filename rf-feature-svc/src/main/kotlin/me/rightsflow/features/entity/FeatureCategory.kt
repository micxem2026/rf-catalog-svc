package me.rightsflow.features.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "klf_feature_category")
data class FeatureCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_seq")
    @SequenceGenerator(name = "category_seq", sequenceName = "klf_feature_category_id_seq", allocationSize = 1)
    val id: Int? = null,

    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Column(name = "created_by", nullable = false, length = 20)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_by", length = 20)
    var updatedBy: String? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
