package me.rightsflow.intersync.scheduler

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import me.rightsflow.intersync.config.MessageConverter
import me.rightsflow.intersync.dto.UserAvroMessage
import me.rightsflow.intersync.service.ReplicationService
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class DlqScheduler(
    @param:Value("\${spring.cloud.stream.kafka.binder.brokers}") // Получаем адрес брокеров
    private val bootstrapServers: String,
    @param:Value("\${spring.cloud.stream.kafka.binder.configuration.schema.registry.url}") // Получаем URL реестра
    private val schemaRegistryUrl: String,
    private val kafkaTemplate: KafkaTemplate<String, GenericRecord?>,
    private val replicationService: ReplicationService,
    @param:Value("\${spring.cloud.stream.kafka.bindings.userProcessor-in-0.consumer.dlq-name}")
    private val dlqTopic: String
) {

    private val log = LoggerFactory.getLogger(DlqScheduler::class.java)

    @Scheduled(cron = "0 0/5 * * * *")
    fun processDlq() {
        log.info("Start processing a DLQ topic: $dlqTopic")

        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "${dlqTopic}.reprocessor-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java.name,
            "schema.registry.url" to schemaRegistryUrl,
            "specific.avro.reader" to "false",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1000",
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to "30000",
            ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG to "40000",
        )

        KafkaConsumer<String, GenericRecord?>(consumerProps).use { consumer ->

            consumer.subscribe(listOf(dlqTopic))

            val records = consumer.poll(Duration.ofSeconds(10))

            if (records.isEmpty) {
                log.info("DLQ topic $dlqTopic is empty")
                return
            }

            val failedRecords = mutableListOf<ConsumerRecord<String, GenericRecord?>>()
            var successCount = 0
            val totalCount = records.count()

            log.info("Received $totalCount messages to process from DLQ")

            // 1. Пытаемся обработать каждое сообщение из пачки
            for (record in records) {
                try {

                    val keyString = record.key()
                    if (keyString == null) {
                        log.warn("A tombstone message without a key was received. The event will be ignored.")
                    }
                    if (keyString != null) {
                        val syncId = keyString.substringAfter("=").substringBefore("}").trim().toInt()
                        val userDto = when (record.value()) {
                            null -> UserAvroMessage(
                                null, "", "", "", "", false,
                                false, false, 0L, 0L, 0L, 0L, ""
                            )

                            is GenericRecord -> MessageConverter.convertToUserAvroMessage(record.value() as GenericRecord)
                        }
                        replicationService.processUser(syncId, userDto)
                        successCount++
                    }
                    log.debug("Successfully processed message for user: ${keyString} [partition=${record.partition()}, offset=${record.offset()}]")
                } catch (e: Exception) {
                    log.error("Error processing message from DLQ for user: ${record.key()} [partition=${record.partition()}, offset=${record.offset()}]. It will be postponed. Error: ${e.message}")
                    failedRecords.add(record)
                }
            }

            // 2. КЛЮЧЕВОЕ УСЛОВИЕ: действуем, только если был хоть какой-то прогресс
            if (successCount > 0) {
                log.info("Processed successfully: $successCount from $totalCount. Failed: ${failedRecords.size}")

                // 3. Возвращаем неудачные сообщения обратно в DLQ
                if (failedRecords.isNotEmpty()) {
                    returnFailedMessagesToDlq(failedRecords)
                }

                // 4. Коммитим смещение для всей пачки
                consumer.commitSync()
                log.info("Offset is committed for the processed batch of $totalCount messages")

            } else {
                // Если successCount == 0, значит вся пачка сбойная
                log.warn("Could not process any messages from $totalCount. No action taken, offset not committed.")
            }
        }
    }

    private fun returnFailedMessagesToDlq(failedRecords: List<ConsumerRecord<String, GenericRecord?>>) {
        log.warn("Returning ${failedRecords.size} messages back to the DLQ...")

        var returnedCount = 0
        var returnErrors = 0

        failedRecords.forEach { record ->
            try {
                kafkaTemplate.send(dlqTopic, record.key(), record.value()).get(5, TimeUnit.SECONDS)
                returnedCount++
            } catch (e: Exception) {
                returnErrors++
                log.error(
                    "Error returning message in DLQ for user: ${record.key()} [offset=${record.offset()}]: ${e.message}",
                    e
                )
            }
        }

        log.info("Completed returning failed messages to DLQ: Successfully returned=$returnedCount, errors=$returnErrors")
    }
}
