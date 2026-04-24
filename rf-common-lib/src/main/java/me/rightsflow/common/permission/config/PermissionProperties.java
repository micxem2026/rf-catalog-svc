package me.rightsflow.common.permission.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Конфигурационные свойства модуля проверки прав.
 *
 * <p>Пример конфигурации в {@code application.yml} микросервиса:</p>
 * <pre>
 * rightsflow:
 *   permissions:
 *     auth-service-url: http://${RF_AUTH_SVC_HOSTNAME:localhost}:9000/auth
 *     refresh-interval: PT5M
 *     startup-mode: WARN
 * </pre>
 *
 * <p>Настройки OAuth2-клиента берутся из уже существующего блока
 * {@code rightsflow.oauth2.client.system}, определённого в {@code application.yml}
 * rf-config-repo, и НЕ дублируются здесь.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "rightsflow.permissions")
public class PermissionProperties {

    /**
     * Базовый URL rf-auth-svc (без trailing slash).
     * Пример: {@code http://auth-svc:9000/auth}
     */
    private String authServiceUrl = "http://localhost:9000/auth";

    /**
     * Интервал принудительного обновления кэша как fallback.
     * По умолчанию 5 минут.
     */
    private Duration refreshInterval = Duration.ofMinutes(5);

    /**
     * Таймаут HTTP-запроса к rf-auth-svc при загрузке прав.
     */
    private Duration clientTimeout = Duration.ofSeconds(5);

    /**
     * Имя Kafka-топика для получения событий инвалидации кэша.
     */
    private String invalidationTopic = "rf.permissions.invalidated";

    /**
     * Имя Kafka consumer group.
     * По умолчанию формируется как {@code <appName>-permissions-group}.
     * Переопределите если нужно кастомное имя.
     */
    private String consumerGroup;

    /**
     * Поведение при недоступности rf-auth-svc во время старта:
     * <ul>
     *   <li>{@code FAIL} — приложение не запустится</li>
     *   <li>{@code WARN} — приложение запустится с пустым кэшем, права будут загружены
     *       при первом обращении или по расписанию</li>
     * </ul>
     */
    private StartupMode startupMode = StartupMode.WARN;

    // ---- Поля системного OAuth2 клиента (читаем из rightsflow.oauth2.client.system) ----
    // Маппинг через вложенный объект SystemClient

    private SystemClient systemClient = new SystemClient();

    @Getter
    @Setter
    public static class SystemClient {
        private String clientId = "system";
        private String clientSecret;
        private String scope = "admin";
    }

    /**
     * Удобные геттеры для обратной совместимости с RestPermissionClient.
     */
    public String getSystemClientId() {
        return systemClient.getClientId();
    }

    public String getSystemClientSecret() {
        return systemClient.getClientSecret();
    }

    public String getSystemClientScope() {
        return systemClient.getScope();
    }

    private Registration registration = new Registration();

    @Getter
    @Setter
    public static class Registration {

        /**
         * Включить авто-регистрацию прав при старте приложения.
         * По умолчанию: true.
         */
        private boolean enabled = true;

        /**
         * Количество попыток регистрации при недоступности rf-auth-svc.
         * По умолчанию: 3.
         */
        private int retryAttempts = 3;

        /**
         * Пауза между попытками.
         * По умолчанию: 2 секунды.
         */
        private Duration retryDelay = Duration.ofSeconds(2);
    }

    public enum StartupMode {
        FAIL, WARN
    }
}
