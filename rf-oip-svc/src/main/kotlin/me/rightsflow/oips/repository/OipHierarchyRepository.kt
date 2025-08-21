package me.rightsflow.oips.repository

import me.rightsflow.oips.entity.OipHierarchy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OipHierarchyRepository : JpaRepository<OipHierarchy, Int> {

    // Дети по ID_PARENT, сортировка по умолчанию — по PART_NUM дочерних ОИС
    @Query(
        value = """
            select h
            from OipHierarchy h
            join Oip o on o.id = h.idOip
            where h.idParent = :parentId
            order by o.partNum asc
        """,
        countQuery = """
            select count(h)
            from OipHierarchy h
            where h.idParent = :parentId
        """
    )
    fun findChildrenSortedByPartNum(
        @Param("parentId") parentId: Int,
        pageable: Pageable
    ): Page<OipHierarchy>

    // Родители по ID_OIP, сортировка по умолчанию — по ID записи иерархии
    fun findByIdOipOrderByIdAsc(idOip: Int, pageable: Pageable): Page<OipHierarchy>
}