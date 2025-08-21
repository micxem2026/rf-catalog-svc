package me.rightsflow.features.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(
    name = "KLF_FEATURE_PLAIN",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ID_FEATURE_CATEGORY", "NAME"])]
)
class FeaturePlain : BaseAudit() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Int? = null

    @Column(name = "NAME", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "ID_FEATURE_CATEGORY", nullable = false)
    var idFeatureCategory: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_FEATURE_CATEGORY", insertable = false, updatable = false)
    val featureCategory: FeatureCategory? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as FeaturePlain

        // Считаем равными только если id != null и совпадает
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)

}
