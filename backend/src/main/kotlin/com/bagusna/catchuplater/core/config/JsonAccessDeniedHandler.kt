package com.bagusna.catchuplater.core.config

import com.bagusna.catchuplater.common.error.ApiErrorWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * Returns a JSON 403 when an authenticated user lacks the required authority.
 */
@Component
class JsonAccessDeniedHandler(
    private val apiErrorWriter: ApiErrorWriter,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        apiErrorWriter.write(
            response = response,
            status = HttpStatus.FORBIDDEN,
            code = "ACCESS_DENIED",
            message = "You do not have permission to access this resource.",
            path = request.requestURI,
        )
    }
}
