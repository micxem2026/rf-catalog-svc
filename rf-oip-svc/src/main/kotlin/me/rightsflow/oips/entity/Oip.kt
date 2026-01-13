package me.rightsflow.oips.entity

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.*
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate
import org.hibernate.annotations.Type
import java.time.Duration

@Entity
@Table(
    name = "KLF_OIP",
    uniqueConstraints = [UniqueConstraint(columnNames = ["GUID"])]
)
class Oip(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    var id: Int? = null,

    @Column(name = "GUID", length = 255, unique = true)
    var guid: String? = null,

    @Column(name = "ID_OIP_SUPER_TYPE", nullable = false)
    var idOipSuperType: Int,

    @Column(name = "ID_OIP_TYPE", nullable = false)
    var idOipType: Int,

    @Column(name = "NAME", nullable = false, length = 512)
    var name: String,

    @Column(name = "PART_NUM", nullable = false)
    var partNum: Int = 0,

    @Column(name = "PART_COUNT", nullable = false)
    var partCount: Int = 0,

    @Type(PostgreSQLIntervalType::class)
    @Column(name = "DURATION", columnDefinition = "INTERVAL")
    var duration: Duration? = null,

    @Column(name = "DESCRIPTION")
    var description: String? = null,

    @Column(name = "HAS_CHILDREN")
    var hasChildren: Boolean = false,

    @Column(name = "HAS_PARENT")
    var hasParent: Boolean = false,

    @Column(name = "CHILDREN_COUNT")
    var childrenCount: Int = 0,

    @Column(name = "NATIVE_NAME")
    var nativeName: String? = null,

    @Column("RELEASE_YEAR")
    var releaseYear: String? = null,

    @Column("FULL_NAME")
    var fullName: String? = null
) : BaseAudit() {

    enum class NodeType {
         ROOT, LEAF, BRANCH, ISOLATED
    }

    enum class HierarchyDirection {
        UP,      // только предки (родители вверх по иерархии)
        DOWN,    // только потомки (дети вниз по иерархии)
        BOTH     // и предки, и потомки (полная иерархия)
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_OIP_SUPER_TYPE", referencedColumnName = "ID", insertable = false, updatable = false)
    var oipSuperType: OipSuperType? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_OIP_TYPE", referencedColumnName = "ID", insertable = false, updatable = false)
    var oipType: OipType? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as Oip

        // Считаем равными только если id != null и совпадает
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}