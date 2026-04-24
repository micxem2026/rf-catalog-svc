package me.rightsflow.common.permission.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для декларативной проверки прав доступа к методам контроллеров.
 * Заменяет @PreAuthorize("hasAuthority('SCOPE_...')").
 *
 * <p>Пример использования:</p>
 * <pre>
 *   @RequiresPermission("ContractController:createContract")
 *   @PostMapping("/contracts")
 *   public ResponseEntity<ContractDto> createContract(...) { ... }
 * </pre>
 *
 * <p>Право проверяется по схеме: {@code <spring.application.name>:<resource>:<action>}.
 * Имя сервиса подставляется автоматически из {@code spring.application.name}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * Право в формате {@code "Resource:action"}.
     * Имя сервиса подставляется автоматически.
     * <p>Пример: {@code "ContractController:createContract"}</p>
     */
    String value();

    /**
     * Описание права.
     * Если не задано — генерируется автоматически при регистрации.
     * Пример: {@code "Создание нового контракта"}
     */
    String description() default "";

    /**
     * Если {@code true} (по умолчанию) — при отсутствии права
     * выбрасывается {@link org.springframework.security.access.AccessDeniedException},
     * что приводит к ответу HTTP 403.
     *
     * <p>Значение {@code false} зарезервировано для будущего использования
     * (условная логика внутри метода).</p>
     */
    boolean required() default true;
}
