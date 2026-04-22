package me.rightsflow.common.permission.client;

/**
 * Исключение, выбрасываемое при невозможности загрузить права из rf-auth-svc.
 */
public class PermissionClientException extends RuntimeException {

    public PermissionClientException(String message) {
        super(message);
    }

    public PermissionClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
