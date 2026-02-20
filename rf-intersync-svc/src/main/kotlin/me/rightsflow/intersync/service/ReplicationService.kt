package me.rightsflow.intersync.service

import me.rightsflow.intersync.dto.KeyMappingAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareObjectAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareSystemAvroMessage
import me.rightsflow.intersync.dto.UserAvroMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReplicationService(
    private val syncService: SyncService
) {

    private val log = LoggerFactory.getLogger(ReplicationService::class.java)

    @Transactional
    fun processUser(syncId: Int, message: UserAvroMessage) {
        try {
            syncService.syncUser(syncId, message)
        } catch (exception: Exception) {
            log.error("Error processing user with id: $syncId", exception)
            throw exception
        }
    }

    @Transactional
    fun processSwSystem(syncId: Int, message: LovSoftwareSystemAvroMessage) {
        try {
            syncService.syncLovSoftwareSystem(syncId, message)
        } catch (exception: Exception) {
            log.error("Error processing user with id: $syncId", exception)
            throw exception
        }
    }

    @Transactional
    fun processSwObject(syncId: Int, message: LovSoftwareObjectAvroMessage) {
        try {
            syncService.syncLovSoftwareObject(syncId, message)
        } catch (exception: Exception) {
            log.error("Error processing user with id: $syncId", exception)
            throw exception
        }
    }

    @Transactional
    fun processKeyMapping(syncId: Long, message: KeyMappingAvroMessage) {
        try {
            syncService.syncKeyMapping(syncId, message)
        } catch (exception: Exception) {
            log.error("Error processing user with id: $syncId", exception)
            throw exception
        }
    }
}