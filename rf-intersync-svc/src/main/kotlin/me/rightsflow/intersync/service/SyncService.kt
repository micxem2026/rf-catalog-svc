package me.rightsflow.intersync.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import me.rightsflow.intersync.dto.KeyMappingAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareSystemAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareObjectAvroMessage
import me.rightsflow.intersync.dto.UserAvroMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
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
            "SELECT pkg_sync.sync_users(" +
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

        println("UPD =>"+dto.updated_at?.let { microsToLocalDateTime(it) })
        //throw RuntimeException("Sync error")
        return query.singleResult as Int
    }

    @Transactional
    fun syncLovSoftwareSystem(pSyncId: Int, dto: LovSoftwareSystemAvroMessage): Int {
        val query = entityManager.createNativeQuery(
            "SELECT pkg_sync.sync_lov_software_system(" +
                    ":pSyncId, " +
                    ":pId, " +
                    ":pName" +
                    ")"
        )

        query.setParameter("pSyncId", pSyncId)
        query.setParameter("pId", dto.id)
        query.setParameter("pName", dto.name)

        return query.singleResult as Int
    }

    @Transactional
    fun syncLovSoftwareObject(pSyncId: Int, dto: LovSoftwareObjectAvroMessage): Int {
        val query = entityManager.createNativeQuery(
            "SELECT pkg_sync.sync_lov_software_object(" +
                    ":pSyncId, " +
                    ":pId, " +
                    ":pName" +
                    ")"
        )

        query.setParameter("pSyncId", pSyncId)
        query.setParameter("pId", dto.id)
        query.setParameter("pName", dto.name)

        return query.singleResult as Int
    }

    @Transactional
    fun syncKeyMapping(pSyncId: Long, dto: KeyMappingAvroMessage): Int {
        val query = entityManager.createNativeQuery(
            "SELECT pkg_sync.sync_key_mapping(" +
                    ":pSyncId, " +
                    ":pId, " +
                    ":pIdSwSys, " +
                    ":pIdSwObj, " +
                    ":pIdRf, " +
                    ":pIdExt, " +
                    ":pCreatedBy, " +
                    ":pCreatedAt, " +
                    ":pUpdatedBy, " +
                    ":pUpdatedAt" +
                    ")"
        )

        query.setParameter("pSyncId", pSyncId)
        query.setParameter("pId", dto.id)
        query.setParameter("pIdSwSys", dto.id_sw_sys)
        query.setParameter("pIdSwObj", dto.id_sw_obj)
        query.setParameter("pIdRf", dto.id_rf)
        query.setParameter("pIdExt", dto.id_ext)
        query.setParameter("pCreatedBy", dto.created_by)
        query.setParameter("pCreatedAt", dto.created_at?.let { microsToOffsetDateTime(it) })
        query.setParameter("pUpdatedBy", dto.updated_by)
        query.setParameter("pUpdatedAt", dto.updated_at?.let { microsToOffsetDateTime(it) })

        return query.singleResult as Int
    }

    private fun microsToLocalDateTime(micros: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(micros / 1_000_000, (micros % 1_000_000) * 1000),
            ZoneOffset.UTC
        )
    }

    private fun microsToOffsetDateTime(micros: Instant): OffsetDateTime {
        return OffsetDateTime.ofInstant(micros, ZoneOffset.UTC)
    }
}