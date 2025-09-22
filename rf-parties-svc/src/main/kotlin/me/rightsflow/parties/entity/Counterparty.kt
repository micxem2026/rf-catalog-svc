package me.rightsflow.parties.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(
    name = "KLF_COUNTERPARTY",
    uniqueConstraints = [UniqueConstraint(columnNames = ["GUID"])]
)
class Counterparty(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    var id: Int? = null,

    @Column(name = "GUID", length = 255, unique = true)
    var guid: String? = null,

    @Column(name = "NAME", nullable = false, length = 255)
    var name: String,

    @Column(name = "ID_ORG_REF", nullable = true)
    var idOrgRef: Int? = null

    ) : BaseAudit() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ORG_REF", referencedColumnName = "ID", insertable = false, updatable = false)
    var organization: Organization? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as Counterparty

        // Считаем равными только если id != null и совпадает
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}