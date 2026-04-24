package me.rightsflow.common.permission.registration;

import java.util.List;

/**
 * DTO для batch-регистрации прав в rf-auth-svc.
 * Отправляется на POST /api/permissions/register-batch.
 */
public record PermissionRegistrationRequest(
        String service,
        List<PermissionEntry> permissions
) {
    public record PermissionEntry(
            String resource,
            String action,
            String description
    ) {}
}