package com.bagusna.catchuplater.features.auth.service

import com.bagusna.catchuplater.features.auth.dto.LoginRequest
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.auth.dto.CurrentUserResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service

/**
 * Explicit, session-based login. This mirrors what Spring Security's own
 * authentication filter does on success:
 *
 *  1. authenticate the credentials,
 *  2. apply session fixation protection (change the session id),
 *  3. publish the security context into the session repository.
 */
@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val sessionAuthenticationStrategy: SessionAuthenticationStrategy,
    private val securityContextRepository: SecurityContextRepository,
    private val currentUserService: CurrentUserService,
) {
    fun login(
        request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): CurrentUserResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.email.trim(), request.password),
        )

        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse)

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, httpRequest, httpResponse)

        val principal = authentication.principal as AppUserPrincipal
        return currentUserService.toResponse(principal)
    }
}
