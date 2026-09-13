package com.bagusna.catchuplater.core.config

import com.bagusna.catchuplater.common.error.ApiErrorWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * Returns a JSON 401 for unauthenticated access to protected resources.
 */
@Component
class JsonAuthenticationEntryPoint(
    private val apiErrorWriter: ApiErrorWriter,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        apiErrorWriter.write(
            response = response,
            status = HttpStatus.UNAUTHORIZED,
            code = "UNAUTHENTICATED",
            message = "Authentication is required to access this resource.",
        )
    }
}
