package me.rightsflow.common.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class SecuritySubjectProvider {
    fun currentSub(): String =
        (SecurityContextHolder.getContext().authentication?.principal as? Jwt)?.subject ?: "anonymous"
}