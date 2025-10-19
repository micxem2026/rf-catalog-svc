package me.rightsflow.clients.config

import feign.RequestInterceptor
import feign.RequestTemplate
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class FeignAuthInterceptor : RequestInterceptor {

    private val log = LoggerFactory.getLogger(FeignAuthInterceptor::class.java)

    override fun apply(template: RequestTemplate) {
        log.debug("Applying Feign auth interceptor")

        // Попытка 1: Извлечь токен из текущего HTTP запроса
        try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val request: HttpServletRequest? = requestAttributes?.request

            if (request != null) {
                val authHeader = request.getHeader("Authorization")
                log.debug("Auth header from request: ${authHeader?.take(20)}...")
                if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
                    template.header("Authorization", authHeader)
                    log.debug("Token added to Feign request from HttpServletRequest")
                    return
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to get token from RequestContextHolder: ${e.message}")
        }

        // Попытка 2: Извлечь токен из SecurityContext
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            log.debug("Authentication from SecurityContext: {}", authentication)
            if (authentication is JwtAuthenticationToken) {
                val token = authentication.token.tokenValue
                template.header("Authorization", "Bearer $token")
                log.debug("Token added to Feign request from SecurityContext")
                return
            }
        } catch (e: Exception) {
            log.warn("Failed to get token from SecurityContext: ${e.message}")
        }

        log.warn("No authentication token found for Feign request")
    }
}