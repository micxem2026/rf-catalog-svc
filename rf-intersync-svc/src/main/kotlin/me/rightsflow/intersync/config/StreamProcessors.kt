package me.rightsflow.intersync.config

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
                log.warn("A tombstone message without a key was received. The message will be ignored.")
            }
            // Извлечение Acknowledgment из заголовков
            val acknowledgment = message.headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment::class.java)
            if (acknowledgment == null) {
                log.warn("No Acknowledgment found in headers for message with id: $keyString")
            }
            if (keyString != null) {
                val syncId = keyString.substringAfter("=").substringBefore("}").trim().toInt()
                log.info("Received sync message with id: $syncId")
                val userDto = when (message.payload) {
                    is GenericRecord -> MessageConverter.convertToUserAvroMessage(message.payload as GenericRecord)
                    is KafkaNull -> UserAvroMessage(null,"","","","",false,
                        false,false,0L,0L,0L,0L,"")
                    else -> throw IllegalArgumentException("Unsupported message type: ${message.payload.javaClass}")
                }
                replicationService.processUser(syncId, userDto)
                acknowledgment?.acknowledge()
                log.info("Successfully processed message with id: ${syncId}")
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