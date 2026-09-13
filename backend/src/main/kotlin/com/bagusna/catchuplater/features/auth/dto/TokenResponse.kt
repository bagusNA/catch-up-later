package com.bagusna.catchuplater.features.auth.dto

/**
 * Bearer token pair returned to the extension.
 *
 * `expiresIn` / `refreshExpiresIn` are lifetimes in seconds, which lets the
 * client schedule a refresh without trusting clock synchronisation.
 */
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshToken: String,
    val refreshExpiresIn: Long,
    val user: CurrentUserResponse,
)
