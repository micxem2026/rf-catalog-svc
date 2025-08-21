package me.rightsflow.oips.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(
    name = "KLF_OIP_HIERARCHY",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ID_PARENT", "ID_OIP"])]
)
class OipHierarchy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    var id: Int,

    @Column(name = "ID_PARENT", nullable = false)
    var idParent: Int,

    @Column(name = "ID_OIP", nullable = false)
    var idOip: Int
) : BaseAudit() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PARENT", referencedColumnName = "ID", insertable = false, updatable = false)
    var parent: Oip? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_OIP", referencedColumnName = "ID", insertable = false, updatable = false)
    var oip: Oip? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as OipHierarchy

        // Считаем равными только если id != null и совпадает
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}