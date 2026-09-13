package com.bagusna.catchuplater.core.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

/**
 * Logout is idempotent: it always returns 204 No Content once the session has
 * been invalidated and the security context cleared by the logout filter.
 */
@Component
class JsonLogoutSuccessHandler : LogoutSuccessHandler {
    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        response.status = HttpServletResponse.SC_NO_CONTENT
    }
}
