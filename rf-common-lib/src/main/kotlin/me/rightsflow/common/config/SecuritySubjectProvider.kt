package me.rightsflow.common.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Провайдер данных текущего пользователя из SecurityContext.
 *
 * Предоставляет username (JWT sub) и список ролей для передачи
 * в PL/pgSQL-функции, реализующие контроль доступа на уровне БД.
 */
@Component
class SecuritySubjectProvider {

    /**
     * Возвращает username текущего пользователя (JWT claim "sub").
     * Если аутентификация отсутствует — возвращает "anonymous".
     */
    fun currentSub(): String =
        (SecurityContextHolder.getContext().authentication?.principal as? Jwt)
            ?.subject ?: "anonymous"

    /**
     * Возвращает массив ролей текущего пользователя **без** префикса ROLE_.
     *
     * Пример: если Spring Security содержит ["ROLE_ADMIN", "ROLE_USER"],
     * метод вернёт ["ADMIN", "USER"].
     *
     * Используется для передачи в параметр p_roles PL/pgSQL-функций
     * pkg_contract.get_user_org_ids и pkg_contract.check_contract_org_access,
     * которые реализуют bypass для ролей ADMIN и SERVICE.
     *
     * @return массив строк с именами ролей (без ROLE_), или пустой массив
     *         если аутентификация отсутствует или у пользователя нет ролей.
     */
    fun currentRoles(): Array<String> {
        val auth = SecurityContextHolder.getContext().authentication
            ?: return emptyArray()

        return auth.authorities
            .map { it.authority }
            .filter { it.startsWith("ROLE_") }
            .map { it.removePrefix("ROLE_") }
            .toTypedArray()
    }

    /**
     * Возвращает TRUE если текущий пользователь имеет роль ADMIN или SERVICE.
     *
     * При p_bypass = TRUE функции пропускают проверку доступа по организациям,
     * предоставляя полный доступ ко всем контрактам.
     */
    fun isBypassRole(): Boolean =
        currentRoles().any { it in listOf("ADMIN", "SERVICE") }
}