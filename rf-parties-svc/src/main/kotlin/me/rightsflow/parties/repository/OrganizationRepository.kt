package me.rightsflow.parties.repository

import me.rightsflow.parties.entity.Organization
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrganizationRepository : JpaRepository<Organization, Int> {

    @Query(
        """
        select o
        from Organization o
        where (lower(o.name) like lower(concat('%', :filter, '%')) or 
               lower(o.guid) like lower(concat('%', :filter, '%')) or 
               :filter is null)
        """
    )
    fun findByFilter(@Param("filter") filter: String?, pageable: Pageable): Page<Organization>
}