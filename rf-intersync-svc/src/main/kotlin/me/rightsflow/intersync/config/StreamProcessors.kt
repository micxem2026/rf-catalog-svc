package me.rightsflow.intersync.config

import me.rightsflow.intersync.dto.KeyMappingAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareObjectAvroMessage
import me.rightsflow.intersync.dto.LovSoftwareSystemAvroMessage
import me.rightsflow.intersync.dto.UserAvroMessage
import me.rightsflow.intersync.service.ReplicationService
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.KafkaNull
import org.springframework.messaging.Message
import java.time.Instant
import java.util.function.Consumer


@Configuration
class StreamProcessors(
    private val replicationService: ReplicationService
) {

    private val log = LoggerFactory.getLogger(StreamProcessors::class.java)


    @Bean
    fun userProcessor(): Consumer<Message<Any>> {
        return Consumer { message ->

            // Извлечение ключа из заголовков
            val keyString = message.headers["kafka_receivedMessageKey"]?.toString()
            if (keyString == null) {
                log.warn("userProcessor -> A tombstone message without a key was received. The message will be ignored.")
            }
            // Извлечение Acknowledgment из заголовков
            val acknowledgment = message.headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java)
            if (acknowledgment == null) {
                log.warn("userProcessor -> No Acknowledgment found in headers for message with id: $keyString")
            }
            if (keyString != null) {
                val syncId = keyString.substringAfter("=").substringBefore("}").trim().toInt()
                log.info("userProcessor -> Received sync message with id: $syncId")
                val userDto = when (message.payload) {
                    is GenericRecord -> MessageConverter.convertToUserAvroMessage(message.payload as GenericRecord)
                    is KafkaNull -> UserAvroMessage(null,"","","","",false,
                        false,false,0L,0L,0L,0L,"")
                    else -> throw IllegalArgumentException("Unsupported message type: ${message.payload.javaClass}")
                }
                replicationService.processUser(syncId, userDto)
                acknowledgment?.acknowledge()
                log.info("userProcessor -> Successfully processed message with id: ${syncId}")
            }
        }
    }

    @Bean
    fun swSystemProcessor(): Consumer<Message<Any>> {
        return Consumer { message ->

            // Извлечение ключа из заголовков
            val keyString = message.headers["kafka_receivedMessageKey"]?.toString()
            if (keyString == null) {
                log.warn("swSystemProcessor -> A tombstone message without a key was received. The message will be ignored.")
            }
            // Извлечение Acknowledgment из заголовков
            val acknowledgment = message.headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java)
            if (acknowledgment == null) {
                log.warn("swSystemProcessor -> No Acknowledgment found in headers for message with id: $keyString")
            }
            if (keyString != null) {
                val syncId = keyString.substringAfter("=").substringBefore("}").trim().toInt()
                log.info("swSystemProcessor -> Received sync message with id: $syncId")
                val swSystemDto = when (message.payload) {
                    is GenericRecord -> MessageConverter.convertToLovSoftwareSystemAvroMessage(message.payload as GenericRecord)
                    is KafkaNull -> LovSoftwareSystemAvroMessage(null,"")
                    else -> throw IllegalArgumentException("swSystemProcessor -> Unsupported message type: ${message.payload.javaClass}")
                }
                replicationService.processSwSystem(syncId, swSystemDto)
                acknowledgment?.acknowledge()
                log.info("swSystemProcessor -> Successfully processed message with id: ${syncId}")
            }
        }
    }

    @Bean
    fun swObjectProcessor(): Consumer<Message<Any>> {
        return Consumer { message ->

            // Извлечение ключа из заголовков
            val keyString = message.headers["kafka_receivedMessageKey"]?.toString()
            if (keyString == null) {
                log.warn("swObjectProcessor -> A tombstone message without a key was received. The message will be ignored.")
            }
            // Извлечение Acknowledgment из заголовков
            val acknowledgment = message.headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java)
            if (acknowledgment == null) {
                log.warn("swObjectProcessor -> No Acknowledgment found in headers for message with id: $keyString")
            }
            if (keyString != null) {
                val syncId = keyString.substringAfter("=").substringBefore("}").trim().toInt()
                log.info("swObjectProcessor -> Received sync message with id: $syncId")
                val swObjectDto = when (message.payload) {
                    is GenericRecord -> MessageConverter.convertToLovSoftwareObjectAvroMessage(message.payload as GenericRecord)
                    is KafkaNull -> LovSoftwareObjectAvroMessage(null,"")
                    else -> throw IllegalArgumentException("swObjectProcessor -> Unsupported message type: ${message.payload.javaClass}")
                }
                replicationService.processSwObject(syncId, swObjectDto)
                acknowledgment?.acknowledge()
                log.info("swObjectProcessor -> Successfully processed message with id: ${syncId}")
            }
        }
    }

    @Bean
    fun keyMappingProcessor(): Consumer<Message<Any>> {
        return Consumer { message ->

            // Извлечение ключа из заголовков
            val keyString = message.headers["kafka_receivedMessageKey"]?.toString()
            if (keyString == null) {
                log.warn("keyMappingProcessor -> A tombstone message without a key was received. The message will be ignored.")
            }
            // Извлечение Acknowledgment из заголовков
            val acknowledgment = message.headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java)
            if (acknowledgment == null) {
                log.warn("keyMappingProcessor -> No Acknowledgment found in headers for message with id: $keyString")
            }
            if (keyString != null) {
                val syncId = keyString.substringAfter("=").substringBefore("}").trim().toLong()
                log.info("keyMappingProcessor -> Received sync message with id: $syncId")
                val keyMappingDto = when (message.payload) {
                    is GenericRecord -> MessageConverter.convertToKeyMappingAvroMessage(message.payload as GenericRecord)
                    is KafkaNull -> KeyMappingAvroMessage(null,1, 1, 0, 0, "",
                        Instant.EPOCH, null, null)
                    else -> throw IllegalArgumentException("keyMappingProcessor -> Unsupported message type: ${message.payload.javaClass}")
                }
                replicationService.processKeyMapping(syncId, keyMappingDto)
                acknowledgment?.acknowledge()
                log.info("keyMappingProcessor -> Successfully processed message with id: ${syncId}")
            }
        }
    }

}

object MessageConverter {

    fun convertToUserAvroMessage(record: GenericRecord): UserAvroMessage {
        return UserAvroMessage(
            id = record.get("id") as Int,
            username = record.getString("username"),
            display_name = record.getString("display_name"),
            email = record.getString("email"),
            password_hash = record.getString("password_hash"),
            enabled = record.get("enabled") as Boolean?,
            account_non_expired = record.get("account_non_expired") as Boolean?,
            account_non_locked = record.get("account_non_locked") as Boolean?,
            expiration_date = record.get("expiration_date") as Long?,
            last_logon = record.get("last_logon") as Long?,
            created_at = record.get("created_at") as Long?,
            updated_at = record.get("updated_at") as Long?,
            user_type = record.getString("user_type")
        )
    }

    fun convertToLovSoftwareSystemAvroMessage(record: GenericRecord): LovSoftwareSystemAvroMessage {
        return LovSoftwareSystemAvroMessage(
            id = record.get("id") as Int?,
            name = record.getString("name")
        )
    }

    fun convertToLovSoftwareObjectAvroMessage(record: GenericRecord): LovSoftwareObjectAvroMessage {
        return LovSoftwareObjectAvroMessage(
            id = record.get("id") as Int?,
            name = record.getString("name")
        )
    }

    fun convertToKeyMappingAvroMessage(record: GenericRecord): KeyMappingAvroMessage {
        return KeyMappingAvroMessage(
            id = record.get("id") as Long?,
            id_sw_sys = record.get("id_sw_sys") as Int,
            id_sw_obj = record.get("id_sw_obj") as Int,
            id_rf = record.get("id_rf") as Long,
            id_ext = record.get("id_ext") as Long,
            created_by = record.getString("created_by"),
            created_at = record.getStringOrNull("created_at")?.let { Instant.parse(it)},
            updated_by = record.getStringOrNull("updated_by"),
            updated_at = record.getStringOrNull("updated_at")?.let { Instant.parse(it)}
        )
    }

    private fun GenericRecord.getStringOrNull(fieldName: String): String? {
        this.schema.getField(fieldName) ?: return null // Поле отсутствует в схеме
        return when (val value = this.get(fieldName)) {
            is Utf8 -> value.toString()
            is String -> value
            null -> null
            else -> value.toString()
        }
    }

    private fun GenericRecord.getString(fieldName: String): String {
        return getStringOrNull(fieldName) ?: ""
    }

    private fun GenericRecord.getRequiredString(fieldName: String): String {
        return getStringOrNull(fieldName) ?: throw IllegalArgumentException("Field $fieldName is required but was null")
    }
}