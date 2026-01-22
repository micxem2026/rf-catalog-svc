package me.rightsflow.righttypes.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import me.rightsflow.features.entity.FeatureCategory
import me.rightsflow.features.entity.FeatureTree
import org.hibernate.Hibernate

@Entity
@Table(
    name = "KLF_FEATURE_CAT_TO_RT",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ID_RIGHT_TYPE", "ID_FEATURE_CATEGORY"])]
)
class FeatureCatToRt : BaseAudit() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Int? = null

    @Column(name = "ID_RIGHT_TYPE", nullable = false)
    var idRightType: Int = 0

    @Column(name = "ID_FEATURE_CATEGORY", nullable = false)
    var idFeatureCategory: Int = 0

    @Column(name = "ID_DEF_FEATURE")
    var idDefaultFeature: Int? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_RIGHT_TYPE", insertable = false, updatable = false)
    val rightType: RightType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_FEATURE_CATEGORY", insertable = false, updatable = false)
    val featureCategory: FeatureCategory? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DEF_FEATURE", insertable = false, updatable = false)
    val defaultFeature: FeatureTree? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as FeatureCatToRt

        // Считаем равными только если id != null и совпадает
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}