package com.bagusna.catchuplater.core.security

import com.bagusna.catchuplater.features.auth.service.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates requests carrying an `Authorization: Bearer <token>` header.
 *
 * The token is validated against the database by [TokenService]; an invalid or
 * expired token simply leaves the request unauthenticated so the standard
 * entry point returns the 401 envelope.
 */
@Component
class BearerTokenAuthenticationFilter(
    private val tokenService: TokenService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(AUTHORIZATION)
        if (header != null &&
            header.startsWith(BEARER_PREFIX) &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val token = header.removePrefix(BEARER_PREFIX).trim()
            val principal = tokenService.authenticateAccess(token)
            if (principal != null) {
                val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = authentication
                SecurityContextHolder.setContext(context)
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val AUTHORIZATION: String = "Authorization"
        const val BEARER_PREFIX: String = "Bearer "
    }
}
