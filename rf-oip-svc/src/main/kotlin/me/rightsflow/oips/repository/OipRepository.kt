package me.rightsflow.oips.repository

import me.rightsflow.oips.entity.Oip
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OipRepository : JpaRepository<Oip, Int> {

    @Query(
        """
        select o
        from Oip o
        where (o.idOipSuperType = :idSuperType or :idSuperType is null)
          and (o.idOipType      = :idType or :idType is null)
          and (o.hasChildren    = :hasChildren or :hasChildren is null)
          and (o.hasParent      = :hasParent or :hasParent is null)
          and (lower(o.name) like lower(concat('%', :nameFilter, '%')) or :nameFilter is null)
        """
    )
    fun findByFilter(
        @Param("idSuperType") idSuperType: Int?,
        @Param("idType") idType: Int?,
        @Param("hasChildren") hasChildren: Boolean?,
        @Param("hasParent") hasParent: Boolean?,
        @Param("nameFilter") nameFilter: String?,
        pageable: Pageable
    ): Page<Oip>
}