package me.rightsflow.intersync.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.intersync.dto.UserAvroMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class SyncService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional
    fun syncUser(pSyncId: Int,
                       dto: UserAvroMessage
    ): Int {
        val query = entityManager.createNativeQuery(
            "SELECT rightsflow.sync_users(" +
                    ":pSyncId, " +
                    ":pId, " +
                    ":pUsername, " +
                    ":pDisplayName, " +
                    ":pEmail, " +
                    ":pPasswordHash, " +
                    ":pEnabled, " +
                    ":pAccountNonExpired, " +
                    ":pAccountNonLocked, " +
                    ":pExpirationDate, " +
                    ":pLastLogon, " +
                    ":pCreatedAt, " +
                    ":pUpdatedAt," +
                    ":pUserType" +
                    ")"
        )

        // Установка параметров
        query.setParameter("pSyncId", pSyncId)
        query.setParameter("pId", dto.id)
        query.setParameter("pUsername", dto.username)
        query.setParameter("pDisplayName", dto.display_name)
        query.setParameter("pEmail", dto.email)
        query.setParameter("pPasswordHash", dto.password_hash)
        query.setParameter("pEnabled", dto.enabled)
        query.setParameter("pAccountNonExpired", dto.account_non_expired)
        query.setParameter("pAccountNonLocked", dto.account_non_locked)
        query.setParameter("pExpirationDate", dto.expiration_date?.let { microsToLocalDateTime(it) })
        query.setParameter("pLastLogon", dto.last_logon?.let { microsToLocalDateTime(it) })
        query.setParameter("pCreatedAt", dto.created_at?.let { microsToLocalDateTime(it) })
        query.setParameter("pUpdatedAt", dto.updated_at?.let { microsToLocalDateTime(it) })
        query.setParameter("pUserType", dto.user_type)
        // Выполнение запроса и возврат результата
        //throw RuntimeException("Sync error")
        return query.singleResult as Int
    }

    private fun microsToLocalDateTime(micros: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(micros / 1_000_000, (micros % 1_000_000) * 1000),
            ZoneOffset.UTC
        )
    }
}