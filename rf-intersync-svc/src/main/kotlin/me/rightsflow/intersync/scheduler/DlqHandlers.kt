package me.rightsflow.intersync.scheduler

import me.rightsflow.intersync.config.MessageConverter
import me.rightsflow.intersync.dto.KeyMappingAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareObjectAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareSystemAvroMessage
import me.rightsflow.intersync.dto.UserAvroMessage
import me.rightsflow.intersync.service.ReplicationService
import org.apache.avro.generic.GenericRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserDlqHandler(
    private val replicationService: ReplicationService,
    @param:Value("\${spring.cloud.stream.kafka.bindings.userProcessor-in-0.consumer.dlq-name}")
    override val topic: String
) : DlqHandler {

    override fun process(key: String, value: GenericRecord?) {
        val syncId = key.substringAfter("=").substringBefore("}").trim().toInt()
        val userDto = when (value) {
            null -> UserAvroMessage(
                null, "", "", "", "", false,
                false, false, 0L, 0L, 0L,
                0L, ""
            )
            else -> MessageConverter.convertToUserAvroMessage(value)
        }
        replicationService.processUser(syncId, userDto)
    }
}

@Component
class SwSystemDlqHandler(
    private val replicationService: ReplicationService,
    @param:Value("\${spring.cloud.stream.kafka.bindings.swSystemProcessor-in-0.consumer.dlq-name}")
    override val topic: String
) : DlqHandler {

    override fun process(key: String, value: GenericRecord?) {
        val syncId = key.substringAfter("=").substringBefore("}").trim().toInt()
        val swSystemDto = when (value) {
            null -> LovSoftwareSystemAvroMessage(
                null, ""
            )
            else -> MessageConverter.convertToLovSoftwareSystemAvroMessage(value)
        }
        replicationService.processSwSystem(syncId, swSystemDto)
    }
}

@Component
class SwObjectDlqHandler(
    private val replicationService: ReplicationService,
    @param:Value("\${spring.cloud.stream.kafka.bindings.swObjectProcessor-in-0.consumer.dlq-name}")
    override val topic: String
) : DlqHandler {

    override fun process(key: String, value: GenericRecord?) {
        val syncId = key.substringAfter("=").substringBefore("}").trim().toInt()
        val swObjectDto = when (value) {
            null -> LovSoftwareObjectAvroMessage(
                null, ""
            )
            else -> MessageConverter.convertToLovSoftwareObjectAvroMessage(value)
        }
        replicationService.processSwObject(syncId, swObjectDto)
    }
}

@Component
class KeyMappingDlqHandler(
    private val replicationService: ReplicationService,
    @param:Value("\${spring.cloud.stream.kafka.bindings.keyMappingProcessor-in-0.consumer.dlq-name}")
    override val topic: String
) : DlqHandler {

    override fun process(key: String, value: GenericRecord?) {
        val syncId = key.substringAfter("=").substringBefore("}").trim().toLong()
        val keyMappingDto = when (value) {
            null -> KeyMappingAvroMessage(null,1, 1, 0, 0, "",
                                          Instant.EPOCH, null, null)
            else -> MessageConverter.convertToKeyMappingAvroMessage(value)
        }
        replicationService.processKeyMapping(syncId, keyMappingDto)
    }
}