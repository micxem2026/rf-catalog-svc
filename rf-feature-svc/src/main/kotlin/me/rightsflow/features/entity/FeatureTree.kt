package me.rightsflow.features.entity

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType
import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDate
import me.rightsflow.common.entity.BaseAudit
import org.hibernate.Hibernate

@Entity
@Table(name = "KLF_FEATURE_TREE")
class FeatureTree: BaseAudit() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Int? = null

    @Column(name = "ID_PARENT")
    var idParent: Int? = null

    @Column(name = "ID_FEATURE_CATEGORY", nullable = false)
    val idFeatureCategory: Int = 0

    @Column(name = "ID_FEATURE_PLAIN", nullable = false)
    var idFeaturePlain: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PARENT", insertable = false, updatable = false)
    val parent: FeatureTree? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_FEATURE_CATEGORY", insertable = false, updatable = false)
    val featureCategory: FeatureCategory? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_FEATURE_PLAIN", insertable = false, updatable = false)
    val featurePlain: FeaturePlain? = null

    //@JdbcTypeCode(SqlTypes.OTHER)
    @Type(PostgreSQLRangeType::class)
    @Column(name = "VALIDITY_PERIOD", columnDefinition = "daterange", nullable = false)
    var validityPeriod: Range<LocalDate> = Range.emptyRange(LocalDate::class.java)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        // Проверка на одинаковый реальный класс (без прокси)
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as FeatureTree

        // Считаем равными только если id != null и совпадает
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: System.identityHashCode(this)
}
