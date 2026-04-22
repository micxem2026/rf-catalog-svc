package me.rightsflow.common.permission.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.common.permission.annotation.RequiresPermission;
import me.rightsflow.common.permission.cache.InMemoryPermissionCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AOP аспект для обработки аннотации {@link RequiresPermission}.
 *
 * <p>Алгоритм проверки права:</p>
 * <ol>
 *   <li>Извлечь роли пользователя из {@link SecurityContextHolder}</li>
 *   <li>Убедиться что роли загружены в кэш (lazy load для новых ролей)</li>
 *   <li>Проверить наличие права в кэше</li>
 *   <li>При отсутствии права — выбросить {@link AccessDeniedException} (→ HTTP 403)</li>
 * </ol>
 *
 * <p>Роли извлекаются из JWT claims в формате {@code ROLE_<NAME>} (Spring Security convention).
 * При проверке префикс {@code ROLE_} отрезается для сравнения с именами ролей в БД.</p>
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    private final InMemoryPermissionCache permissionCache;

    @Around("@annotation(me.rightsflow.common.permission.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1. Получаем метаданные аннотации
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        String permissionValue = annotation.value();

        // 2. Парсим "Resource:action"
        String[] parts = permissionValue.split(":", 2);
        if (parts.length != 2) {
            log.error("Invalid @RequiresPermission value '{}' on method {}.{}. " +
                            "Expected format: 'Resource:action'",
                    permissionValue,
                    joinPoint.getTarget().getClass().getSimpleName(),
                    method.getName());
            throw new IllegalArgumentException(
                    "Invalid @RequiresPermission format: '" + permissionValue +
                            "'. Expected 'Resource:action'");
        }
        String resource = parts[0];
        String action = parts[1];

        // 3. Получаем аутентификацию из SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to {}.{}",
                    joinPoint.getTarget().getClass().getSimpleName(), method.getName());
            throw new AccessDeniedException("Authentication required");
        }

        /*Set<String> roles = new HashSet<>();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            Map<String, Object> claims = jwt.getClaims();
            Object rolesObj = claims.get("roles");

            if (rolesObj != null) {
                if (rolesObj instanceof List) {
                    // Роли представлены как список
                    roles = ((List<?>) rolesObj).stream()
                            .map(Object::toString)
                            .filter(auth -> auth.startsWith("ROLE_"))
                            .map(auth -> auth.substring("ROLE_".length()))
                            .collect(Collectors.toSet());
                    //roles.forEach(System.out::println);
                }  else {
                    // Другие форматы
                    log.warn("Unsupported roles format: {}", rolesObj.getClass());
                }
            }
        }*/

        // 4. Извлекаем роли (убираем префикс ROLE_ который добавляет Spring Security)
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring("ROLE_".length()))
                .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            log.warn("User '{}' has no roles, denying access to {}.{}",
                    authentication.getName(),
                    joinPoint.getTarget().getClass().getSimpleName(),
                    method.getName());
            throw new AccessDeniedException("Access denied: no roles assigned");
        }

        // 5. Lazy load: загружаем права для ролей, которых нет в кэше
        permissionCache.ensureLoaded(roles);

        // 6. Проверяем право
        boolean hasPermission = permissionCache.hasPermission(roles, resource, action);

        if (!hasPermission) {
            log.warn("Access denied for user '{}' with roles {} to {}.{} " +
                            "(required permission: {}:{})",
                    authentication.getName(), roles,
                    joinPoint.getTarget().getClass().getSimpleName(), method.getName(),
                    resource, action);
            throw new AccessDeniedException(
                    "Access denied: missing permission '" + resource + ":" + action + "'");
        }

        log.debug("Access granted for user '{}' to {}.{} (permission: {}:{})",
                authentication.getName(),
                joinPoint.getTarget().getClass().getSimpleName(),
                method.getName(),
                resource, action);

        // 7. Выполняем метод
        return joinPoint.proceed();
    }
}
