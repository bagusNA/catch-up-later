package com.bagusna.catchuplater.features.auth.dto

/**
 * CSRF token bootstrap payload for browser clients. The token is also written to
 * the `XSRF-TOKEN` cookie, but exposing it here avoids a race between the first
 * cookie write and the first state-changing request.
 */
data class CsrfTokenResponse(
    val headerName: String,
    val parameterName: String,
    val token: String,
)
