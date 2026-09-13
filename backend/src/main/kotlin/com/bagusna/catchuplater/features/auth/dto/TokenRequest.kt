package com.bagusna.catchuplater.features.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Password grant used by the browser extension to obtain bearer tokens.
 * The web client uses the session-based `/auth/login` endpoint instead.
 */
data class TokenRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(max = 256)
    val password: String,
)
