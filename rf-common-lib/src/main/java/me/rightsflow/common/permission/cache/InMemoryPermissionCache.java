package me.rightsflow.common.permission.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.common.permission.client.PermissionClient;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Реализация {@link PermissionCache} на основе {@link ConcurrentHashMap}.
 *
 * <p>Структура кэша: {@code Map<roleName, Set<"resource:action">>}.</p>
 *
 * <p>Thread-safety: операции чтения ({@link #hasPermission}) полностью
 * неблокирующие — читают из volatile-ссылки на immutable snapshot.
 * Операции записи ({@link #invalidate}, {@link #refresh}) синхронизированы.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class InMemoryPermissionCache implements PermissionCache {

    private final PermissionClient permissionClient;

    /**
     * Основной кэш: roleName → Set<"resource:action">.
     * Используем ConcurrentHashMap для безопасного чтения без блокировок.
     */
    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    @Override
    public boolean hasPermission(Set<String> roles, String resource, String action) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        String permission = resource + ":" + action;
        return roles.stream()
                .anyMatch(role -> {
                    Set<String> rolePermissions = cache.get(role);
                    return rolePermissions != null && rolePermissions.contains(permission);
                });
    }

    @Override
    public synchronized void invalidate(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        log.info("Invalidating permission cache for roles: {}", roleNames);

        // Удаляем устаревшие записи
        roleNames.forEach(cache::remove);

        // Сразу перезагружаем права для этих ролей
        loadPermissionsForRoles(roleNames);
    }

    @Override
    public synchronized void refresh() {
        log.info("Refreshing full permission cache. Current roles in cache: {}", cache.keySet());
        Set<String> knownRoles = Set.copyOf(cache.keySet());
        if (!knownRoles.isEmpty()) {
            // Перезагружаем только известные роли
            loadPermissionsForRoles(knownRoles);
        }
        // Примечание: новые роли, которых ещё нет в кэше, будут загружены
        // при первом обращении пользователя с такой ролью (lazy loading в аспекте)
    }

    /**
     * Загружает права для указанных ролей из rf-auth-svc и обновляет кэш.
     * Должен вызываться из синхронизированного контекста.
     */
    private void loadPermissionsForRoles(Set<String> roleNames) {
        try {
            Map<String, Set<String>> loaded = permissionClient.fetchPermissionsForRoles(roleNames);
            cache.putAll(loaded);
            log.debug("Loaded permissions for roles {}: {}",
                    roleNames,
                    loaded.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue().size() + " permissions")
                            .collect(Collectors.joining(", ")));
        } catch (Exception e) {
            log.error("Failed to load permissions for roles {}. Cache may be stale.", roleNames, e);
            // Не бросаем исключение — работаем со старым кэшем или пустым
            // Следующий refresh попробует снова
        }
    }

    /**
     * Загружает права для ролей пользователя, которых нет в кэше (lazy loading).
     * Вызывается из {@link me.rightsflow.common.permission.aspect.PermissionAspect}
     * при первом обращении пользователя с новой ролью.
     *
     * @param roles роли из JWT токена текущего пользователя
     */
    public synchronized void ensureLoaded(Set<String> roles) {
        Set<String> missingRoles = roles.stream()
                .filter(role -> !cache.containsKey(role))
                .collect(Collectors.toSet());

        if (!missingRoles.isEmpty()) {
            log.info("Loading permissions for new roles: {}", missingRoles);
            loadPermissionsForRoles(missingRoles);
        }
    }

    /**
     * Возвращает текущее содержимое кэша (только для диагностики/actuator).
     */
    public Map<String, Set<String>> getCacheSnapshot() {
        return Collections.unmodifiableMap(cache);
    }
}
