package me.rightsflow.righttypes.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(name = "KLF_RIGHT_TYPE")
class RightType : BaseAudit() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Int? = null

    @Column(name = "ID_PARENT")
    var parentId: Int? = null

    @Column(name = "NAME", nullable = false, length = 255)
    var name: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PARENT", insertable = false, updatable = false)
    val parent: RightType? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as RightType

        // Считаем равными только если id != null и совпадает
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}