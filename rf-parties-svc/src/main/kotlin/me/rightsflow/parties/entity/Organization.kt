package me.rightsflow.parties.entity

import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(
    name = "KLF_ORGANIZATION",
    uniqueConstraints = [UniqueConstraint(columnNames = ["GUID"])]
)
class Organization(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    var id: Int? = null,

    @Column(name = "GUID", length = 255, unique = true)
    var guid: String? = null,

    @Column(name = "CODE_1C", length = 50, unique = true)
    var code_1c: String? = null,

    @Column(name = "COUNTRY", length = 32)
    var country: String?,

    @Column(name = "ADDRESS")
    var address: String?,

    @Column(name = "TIN", length = 32)
    var tin: String?,

    @Column(name = "NAME", nullable = false, length = 255)
    var name: String,

    ) : BaseAudit() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as Organization

        // Считаем равными только если id != null и совпадает
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}