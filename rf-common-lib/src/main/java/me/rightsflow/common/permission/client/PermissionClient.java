package me.rightsflow.common.permission.client;

import me.rightsflow.common.permission.registration.PermissionRegistrationRequest;
import me.rightsflow.common.permission.registration.PermissionRegistrationResponse;

import java.util.Map;
import java.util.Set;

/**
 * HTTP-клиент для загрузки прав из rf-auth-svc.
 *
 * <p>Запрашивает права только для текущего сервиса
 * (фильтрация по {@code spring.application.name} выполняется на стороне rf-auth-svc).
 * Это гарантирует, что кэш микросервиса содержит только релевантные права.</p>
 */
public interface PermissionClient {

    /**
     * Загружает права для указанных ролей из rf-auth-svc.
     *
     * <p>Вызывает {@code GET /api/permissions/by-roles?roles=ROLE1,ROLE2&service=<appName>}.</p>
     *
     * @param roleNames имена ролей (без префикса ROLE_)
     * @return Map: roleName → Set<"resource:action">.
     *         Если роль существует, но не имеет прав — возвращается пустой Set.
     *         Если rf-auth-svc недоступен — выбрасывается исключение.
     */
    Map<String, Set<String>> fetchPermissionsForRoles(Set<String> roleNames);

    /**
     * Batch upsert прав в rf-auth-svc.
     * Вызывается из {@link me.rightsflow.common.permission.registration.PermissionRegistrar}
     * при старте микросервиса.
     *
     * <p>Операция идемпотентна: уже существующие права не дублируются.</p>
     *
     * @param request список прав для регистрации с именем сервиса
     * @throws PermissionClientException если rf-auth-svc недоступен
     */
    PermissionRegistrationResponse registerPermissions(PermissionRegistrationRequest request);
}
