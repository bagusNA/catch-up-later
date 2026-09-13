package com.bagusna.catchuplater.features.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(max = 256)
    val password: String,
)
