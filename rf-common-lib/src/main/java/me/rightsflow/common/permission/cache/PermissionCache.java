package me.rightsflow.common.permission.cache;

import java.util.Set;

/**
 * Локальный кэш прав доступа для текущего микросервиса.
 *
 * <p>Хранит права в формате {@code Map<roleName, Set<"resource:action">>}.
 * Права загружаются из rf-auth-svc при старте и периодически обновляются.
 * При получении Kafka-события инвалидации кэш сбрасывается для затронутых ролей
 * и немедленно перезагружается.</p>
 */
public interface PermissionCache {

    /**
     * Проверяет, имеет ли хотя бы одна из указанных ролей
     * право на выполнение действия в текущем сервисе.
     *
     * @param roles    набор ролей пользователя, извлечённых из JWT
     * @param resource имя ресурса (например, {@code "ContractController"})
     * @param action   действие  (например, {@code "createContract"})
     * @return {@code true} если хотя бы одна роль имеет указанное право
     */
    boolean hasPermission(Set<String> roles, String resource, String action);

    /**
     * Инвалидирует кэш для указанных ролей и немедленно перезагружает их права.
     * Вызывается при получении Kafka-события {@code rf.permissions.invalidated}.
     *
     * @param roleNames имена ролей, права которых изменились
     */
    void invalidate(Set<String> roleNames);

    /**
     * Принудительно обновляет весь кэш для всех известных ролей.
     * Вызывается по расписанию как fallback на случай пропущенных Kafka-событий.
     */
    void refresh();
}
