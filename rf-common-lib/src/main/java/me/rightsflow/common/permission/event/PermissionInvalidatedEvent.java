package me.rightsflow.common.permission.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Set;

/**
 * Kafka-событие инвалидации кэша прав доступа.
 *
 * <p>Публикуется сервисом rf-auth-svc в топик {@code rf.permissions.invalidated}
 * при любом изменении прав роли (добавление, удаление права, удаление роли).</p>
 *
 * <p>Сериализация: JSON (не Avro — событие маленькое, Schema Registry избыточен).
 * Используется стандартный Spring Kafka {@code JsonDeserializer}.</p>
 *
 * <p>Пример JSON:</p>
 * <pre>
 * {
 *   "role_names": ["MANAGER", "VIEWER"],
 *   "changed_at": "2026-04-20T10:00:00Z"
 * }
 * </pre>
 */
public record PermissionInvalidatedEvent(

        @JsonProperty("role_names")
        Set<String> roleNames,

        @JsonProperty("changed_at")
        Instant changedAt
) {
    /**
     * Фабричный метод для создания события при изменении одной роли.
     */
    public static PermissionInvalidatedEvent of(String roleName) {
        return new PermissionInvalidatedEvent(Set.of(roleName), Instant.now());
    }

    /**
     * Фабричный метод для создания события при изменении нескольких ролей.
     */
    public static PermissionInvalidatedEvent of(Set<String> roleNames) {
        return new PermissionInvalidatedEvent(Set.copyOf(roleNames), Instant.now());
    }
}
