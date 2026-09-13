package com.bagusna.catchuplater.features.auth.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.features.auth.dto.TokenRefreshRequest
import com.bagusna.catchuplater.features.auth.dto.TokenRequest
import com.bagusna.catchuplater.features.auth.dto.TokenResponse
import com.bagusna.catchuplater.features.auth.dto.TokenRevokeRequest
import com.bagusna.catchuplater.features.auth.service.TokenService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Bearer token endpoints used by the browser extension. They are CSRF-exempt
 * because the extension has no session cookie.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/auth/token")
class TokenController(
    private val tokenService: TokenService,
) {
    @PostMapping
    fun token(@Valid @RequestBody request: TokenRequest): TokenResponse =
        tokenService.issueForCredentials(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: TokenRefreshRequest): TokenResponse =
        tokenService.refresh(request)

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@Valid @RequestBody request: TokenRevokeRequest) {
        tokenService.revoke(request)
    }
}
