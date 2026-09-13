package com.bagusna.catchuplater.features.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank
    @field:Size(max = 256)
    val currentPassword: String,

    // The configured minimum length is enforced by the application service so
    // that the policy stays configuration-driven.
    @field:NotBlank
    @field:Size(min = 1, max = 256)
    val newPassword: String,
)
