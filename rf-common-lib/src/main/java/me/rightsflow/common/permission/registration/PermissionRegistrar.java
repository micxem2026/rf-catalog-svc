package me.rightsflow.common.permission.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.common.permission.annotation.RequiresPermission;
import me.rightsflow.common.permission.client.PermissionClient;
import me.rightsflow.common.permission.client.PermissionClientException;
import me.rightsflow.common.permission.config.PermissionProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Сканирует все @RestController-бины из пакета {@code me.rightsflow.**},
 * собирает методы с аннотацией {@link RequiresPermission} и регистрирует
 * найденные права в rf-auth-svc через batch upsert.
 *
 * <p>Запускается на {@link ApplicationReadyEvent} — после полной инициализации
 * Spring-контекста, чтобы все бины были гарантированно доступны.</p>
 *
 * <p>Регистрация идемпотентна: повторный деплой не создаёт дублей,
 * так как rf-auth-svc реализует upsert по уникальному индексу
 * {@code (service, resource, action)}.</p>
 *
 * <p>При недоступности rf-auth-svc выполняется retry согласно настройкам
 * {@code rightsflow.permissions.registration.retry-attempts} и
 * {@code rightsflow.permissions.registration.retry-delay}. После исчерпания
 * попыток поведение определяется {@link PermissionProperties.StartupMode}:
 * FAIL — бросает исключение, WARN — логирует предупреждение и продолжает.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionRegistrar {

    private static final String RF_BASE_PACKAGE = "me.rightsflow.";

    private final ApplicationContext applicationContext;
    private final PermissionClient permissionClient;
    private final PermissionProperties properties;
    private final String applicationName;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.getRegistration().isEnabled()) {
            log.info("[PermissionRegistrar] Auto-registration is disabled " +
                    "(rightsflow.permissions.registration.enabled=false). Skipping.");
            return;
        }

        log.info("[PermissionRegistrar] Starting permission auto-registration for service '{}'", applicationName);

        List<PermissionRegistrationRequest.PermissionEntry> entries = scanPermissions();

        if (entries.isEmpty()) {
            log.info("[PermissionRegistrar] No @RequiresPermission annotations found. " +
                     "Nothing to register.");
            return;
        }

        log.info("[PermissionRegistrar] Found {} permission(s) to register.", entries.size());
        entries.forEach(e ->
                log.debug("[PermissionRegistrar]   → {}:{}", e.resource(), e.action()));

        PermissionRegistrationRequest request = new PermissionRegistrationRequest(applicationName, entries);

        registerWithRetry(request);
    }

    /**
     * Сканирует все @RestController бины из пакета me.rightsflow.**
     * и собирает методы, помеченные @RequiresPermission.
     */
    private List<PermissionRegistrationRequest.PermissionEntry> scanPermissions() {
        List<PermissionRegistrationRequest.PermissionEntry> entries = new ArrayList<>();

        // Получаем все бины с аннотацией @RestController
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(RestController.class);

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = unwrapProxyClass(bean);

            // Фильтруем: только классы из пакета me.rightsflow.**
            if (!isRightsFlowClass(targetClass)) {
                log.trace("[PermissionRegistrar] Skipping non-rightsflow bean: {}", targetClass.getName());
                continue;
            }

            log.debug("[PermissionRegistrar] Scanning controller: {}", targetClass.getSimpleName());

            for (Method method : targetClass.getDeclaredMethods()) {
                RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
                if (annotation == null) {
                    continue;
                }

                String value = annotation.value(); // формат "Resource:action"
                String[] parts = value.split(":", 2);
                if (parts.length != 2) {
                    log.warn("[PermissionRegistrar] Invalid @RequiresPermission value '{}' " +
                                    "on {}.{}. Expected 'Resource:action'. Skipping.",
                            value, targetClass.getSimpleName(), method.getName());
                    continue;
                }

                String resource = parts[0];
                String action   = parts[1];
                String description = annotation.description().isBlank()
                        ? buildDefaultDescription(resource, action)
                        : annotation.description();

                entries.add(new PermissionRegistrationRequest.PermissionEntry(resource, action, description));

                log.debug("[PermissionRegistrar]   Found: {}:{} on {}.{}()",
                        resource, action, targetClass.getSimpleName(), method.getName());
            }
        }

        return entries;
    }

    /**
     * Отправляет batch-запрос регистрации с retry-логикой.
     */
    private void registerWithRetry(PermissionRegistrationRequest request) {
        PermissionProperties.Registration reg = properties.getRegistration();
        int maxAttempts = reg.getRetryAttempts();
        long delayMs    = reg.getRetryDelay().toMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                PermissionRegistrationResponse response =
                        permissionClient.registerPermissions(request);

                // Формируем информативное сообщение на основе ответа сервера
                boolean nothingChanged = response.created() == 0 && response.updated() == 0;

                if (nothingChanged) {
                    log.info("[PermissionRegistrar] Permission registration complete for '{}' " +
                                    "(attempt {}/{}): all {} permission(s) are up to date, nothing changed.",
                            applicationName, attempt, maxAttempts, response.total());
                } else {
                    log.info("[PermissionRegistrar] Permission registration complete for '{}' " +
                                    "(attempt {}/{}): total={}, created={}, updated(description)={}, skipped={}.",
                            applicationName, attempt, maxAttempts,
                            response.total(), response.created(), response.updated(), response.skipped());
                }
                return;

            } catch (PermissionClientException e) {
                log.warn("[PermissionRegistrar] Attempt {}/{} failed: {}",
                        attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    log.info("[PermissionRegistrar] Retrying in {}ms...", delayMs);
                    sleep(delayMs);
                }
            }
        }

        // Все попытки исчерпаны
        String errorMsg = String.format(
                "[PermissionRegistrar] Failed to register permissions for service '%s' " +
                        "after %d attempt(s). Permissions may be missing in rf-auth-svc.",
                applicationName, maxAttempts);

        if (properties.getStartupMode() == PermissionProperties.StartupMode.FAIL) {
            throw new IllegalStateException(errorMsg);
        } else {
            log.error(errorMsg);
        }
    }

    /**
     * Разворачивает Spring AOP-прокси до реального класса.
     * Нужно для корректного чтения аннотаций с прокси-обёрнутых контроллеров.
     */
    private Class<?> unwrapProxyClass(Object bean) {
        try {
            // Spring AOP Proxy → реальный класс
            Class<?> clazz = org.springframework.aop.support.AopUtils.getTargetClass(bean);
            return clazz;
        } catch (Exception e) {
            return bean.getClass();
        }
    }

    /**
     * Проверяет, принадлежит ли класс пакету me.rightsflow.**.
     */
    private boolean isRightsFlowClass(Class<?> clazz) {
        String className = clazz.getName();
        return className.startsWith(RF_BASE_PACKAGE);
    }

    private String buildDefaultDescription(String resource, String action) {
        return String.format("Auto-registered: %s:%s:%s", applicationName, resource, action);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("[PermissionRegistrar] Retry sleep interrupted.");
        }
    }
}
