package com.bagusna.catchuplater.features.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegistrationRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 320)
    val email: String,

    // The configured minimum length is enforced by the application service so
    // that the policy is configuration-driven; this only bounds the payload.
    @field:NotBlank
    @field:Size(min = 1, max = 256)
    val password: String,

    @field:Size(max = 100)
    val displayName: String? = null,
)
