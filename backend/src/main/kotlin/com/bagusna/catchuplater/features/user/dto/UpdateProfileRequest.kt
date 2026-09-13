package com.bagusna.catchuplater.features.user.dto

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(max = 100)
    val displayName: String? = null,
)
