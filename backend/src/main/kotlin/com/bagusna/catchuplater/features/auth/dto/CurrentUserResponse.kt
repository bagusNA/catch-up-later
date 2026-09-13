package com.bagusna.catchuplater.features.auth.dto

/**
 * Public representation of the authenticated/registered user. Never exposes the
 * password hash or other persistence details.
 */
data class CurrentUserResponse(
    val id: Int,
    val email: String,
    val displayName: String?,
    val roles: List<String>,
)
