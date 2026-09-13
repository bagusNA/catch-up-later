package com.bagusna.catchuplater.features.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TokenRevokeRequest(
    @field:NotBlank
    @field:Size(max = 512)
    val refreshToken: String,
)
