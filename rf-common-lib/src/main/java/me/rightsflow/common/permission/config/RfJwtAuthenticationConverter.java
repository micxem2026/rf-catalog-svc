package me.rightsflow.common.permission.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Конвертер claim "roles" из JWT в набор GrantedAuthority.
 *
 * <p>Используется как grantedAuthoritiesConverter внутри стандартного
 * Spring {@code JwtAuthenticationConverter}. Читает claim {@code roles}
 * (формат: {@code ["ROLE_MANAGER", "ROLE_USER"]}) вместо {@code scope}.</p>
 *
 * <p>Нормализация: если значение уже содержит префикс {@code ROLE_} —
 * используется как есть; если нет — префикс добавляется автоматически.</p>
 */
@Slf4j
public class RfJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);

        if (roles == null || roles.isEmpty()) {
            String sub = jwt.getSubject();
            log.warn("JWT for '{}' has no '{}' claim. " +
                     "Check rf-auth-svc token customizer includes roles.", sub, ROLES_CLAIM);
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>(roles.size());
        for (String role : roles) {
            if (role == null || role.isBlank()) continue;
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(authority));
        }

        log.debug("JWT roles claim {} -> authorities {}", roles, authorities);
        return authorities;
    }
}
