package me.rightsflow.common.permission.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.common.permission.cache.InMemoryPermissionCache;
import me.rightsflow.common.permission.event.PermissionInvalidatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Kafka consumer для событий инвалидации кэша прав доступа.
 *
 * <p>Слушает топик {@code rf.permissions.invalidated} и при получении события
 * немедленно инвалидирует локальный кэш для затронутых ролей.</p>
 *
 * <p>Использует чистый Spring Kafka ({@link KafkaListener}) без Spring Cloud Stream,
 * чтобы минимизировать зависимости библиотеки.</p>
 *
 * <p>Consumer group формируется как {@code <appName>-permissions-group} — каждый
 * экземпляр микросервиса получает все события независимо.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionInvalidationListener {

    private final InMemoryPermissionCache permissionCache;

    @KafkaListener(
            topics = "${rightsflow.permissions.invalidation-topic:rf.permissions.invalidated}",
            groupId = "${rightsflow.permissions.consumer-group:${spring.application.name}-permissions-group}",
            containerFactory = "permissionKafkaListenerContainerFactory"
    )
    public void onPermissionInvalidated(
            ConsumerRecord<String, PermissionInvalidatedEvent> record,
            Acknowledgment acknowledgment) {

        PermissionInvalidatedEvent event = record.value();

        if (event == null || event.roleNames() == null || event.roleNames().isEmpty()) {
            log.warn("Received empty permission invalidation event, ignoring");
            acknowledgment.acknowledge();
            return;
        }

        log.info("Received permission invalidation event for roles: {}, changedAt: {}", event.roleNames(), event.changedAt());

        try {
            permissionCache.invalidate(event.roleNames());
            log.info("Successfully invalidated and reloaded permissions for roles: {}", event.roleNames());
        } catch (Exception e) {
            // Логируем, но не бросаем — иначе Kafka будет бесконечно ретраить
            // Следующий scheduled refresh восстановит кэш
            log.error("Failed to invalidate permissions for roles: {}. Cache will be refreshed on next schedule.",
                    event.roleNames(), e);
        } finally {
            // Всегда подтверждаем offset — partial failure не должен блокировать топик
            acknowledgment.acknowledge();
        }
    }
}
