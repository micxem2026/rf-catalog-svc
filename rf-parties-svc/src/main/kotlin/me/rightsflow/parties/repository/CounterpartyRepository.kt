package me.rightsflow.parties.repository

import me.rightsflow.parties.entity.Counterparty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CounterpartyRepository : JpaRepository<Counterparty, Int> {

    @Query(
        """
        select o
        from Counterparty o
        where (lower(o.name) like lower(concat('%', :filter, '%')) or 
               lower(o.guid) like lower(concat('%', :filter, '%')) or 
               :filter is null)
        """
    )
    fun findByFilter(@Param("filter") filter: String?, pageable: Pageable): Page<Counterparty>
}
