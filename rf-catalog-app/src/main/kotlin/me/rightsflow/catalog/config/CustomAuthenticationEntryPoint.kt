package me.rightsflow.catalog.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.rightsflow.features.exception.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val errorResponse = ErrorResponse(
            requestUri = request.requestURI,
            sub = "anonymous", // или из request, если нужно
            status = HttpStatus.UNAUTHORIZED.value(),
            message = HttpStatus.UNAUTHORIZED.reasonPhrase,
            timestamp = LocalDateTime.now()
        )

        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/json"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}