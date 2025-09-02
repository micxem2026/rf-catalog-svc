package me.rightsflow.intersync.service

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
            /*    @Retryable(
                    value = [Exception::class],
                    maxAttempts = 3,
                    backoff = Backoff(delay = 1000, multiplier = 2.0)
                )*/
    fun processUser(syncId: Int, message: UserAvroMessage) {
        try {
            syncService.syncUser(syncId, message)
        } catch (exception: Exception) {
            log.error("Error processing user with id: $syncId", exception)
            throw exception
        }
    }
}