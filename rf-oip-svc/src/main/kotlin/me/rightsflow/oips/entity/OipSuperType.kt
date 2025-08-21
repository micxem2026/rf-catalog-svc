package me.rightsflow.oips.entity

import jakarta.persistence.*
import org.hibernate.Hibernate

@Entity
@Table(
    name = "LOV_OIP_SUPER_TYPE",
    uniqueConstraints = [UniqueConstraint(columnNames = ["NAME"])]
)
class OipSuperType(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Int = 0,

    @Column(name = "NAME", nullable = false, length = 255)
    val name: String

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as OipSuperType

        // Считаем равными только если id != null и совпадает
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}