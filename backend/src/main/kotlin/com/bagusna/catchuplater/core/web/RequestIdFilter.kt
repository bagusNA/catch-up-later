package com.bagusna.catchuplater.core.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Assigns a correlation id to every request.
 *
 * The id is echoed in the `X-Request-ID` response header, stored as a request
 * attribute and put into the logging MDC so that both MVC and Spring Security
 * filter errors can include it in the error envelope.
 *
 * A client-supplied id is accepted only when it matches a conservative
 * character set, which keeps it safe for logs and headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = resolve(request)
        request.setAttribute(ATTRIBUTE, requestId)
        response.setHeader(HEADER, requestId)
        MDC.put(MDC_KEY, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    private fun resolve(request: HttpServletRequest): String =
        request.getHeader(HEADER)
            ?.takeIf { VALID_REQUEST_ID.matches(it) }
            ?: "req_${UUID.randomUUID().toString().replace("-", "").take(24)}"

    companion object {
        const val HEADER: String = "X-Request-ID"
        const val ATTRIBUTE: String = "catchUpLater.requestId"
        const val MDC_KEY: String = "requestId"

        private val VALID_REQUEST_ID = Regex("^[A-Za-z0-9._-]{1,128}$")

        /** The current correlation id, or a stable placeholder outside a request. */
        fun currentId(): String = MDC.get(MDC_KEY) ?: "req_unknown"
    }
}
