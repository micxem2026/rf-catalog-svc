package me.rightsflow.features.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(name = "KLF_FEATURE_CATEGORY")
class FeatureCategory : BaseAudit() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Int? = null

    @Column(name = "NAME", nullable = false, length = 50)
    var name: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as FeatureCategory

        // Считаем равными только если id != null и совпадает
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}
