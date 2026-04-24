package me.rightsflow.common.permission.registration;

/**
 * Ответ rf-auth-svc на batch-регистрацию прав.
 * Аналог серверного me.rightsflow.auth.dto.PermissionRegistrationResponse.
 */
public record PermissionRegistrationResponse(
        String service,
        int total,
        int created,
        int updated,
        int skipped
) {}