package com.bagusna.catchuplater.features.auth.controller

import com.bagusna.catchuplater.features.auth.dto.CsrfTokenResponse
import com.bagusna.catchuplater.features.auth.dto.LoginRequest
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.auth.dto.CurrentUserResponse
import com.bagusna.catchuplater.features.auth.dto.RegistrationRequest
import com.bagusna.catchuplater.features.auth.service.AuthService
import com.bagusna.catchuplater.features.auth.service.CurrentUserService
import com.bagusna.catchuplater.features.auth.service.UserRegistrationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val userRegistrationService: UserRegistrationService,
    private val currentUserService: CurrentUserService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegistrationRequest): CurrentUserResponse =
        currentUserService.toResponse(userRegistrationService.register(request))

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): CurrentUserResponse =
        authService.login(request, httpRequest, httpResponse)

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AppUserPrincipal): CurrentUserResponse =
        currentUserService.toResponse(principal)

    @GetMapping("/csrf")
    fun csrf(csrfToken: CsrfToken): CsrfTokenResponse =
        CsrfTokenResponse(
            headerName = csrfToken.headerName,
            parameterName = csrfToken.parameterName,
            token = csrfToken.token,
        )
}
