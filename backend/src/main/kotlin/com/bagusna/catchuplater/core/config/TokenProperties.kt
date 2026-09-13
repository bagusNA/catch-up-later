package com.bagusna.catchuplater.core.config

import jakarta.validation.constraints.NotNull
import java.time.Duration

/**
 * Lifetime configuration for bearer tokens issued to the browser extension.
 *
 * Access tokens are short-lived; refresh tokens are long-lived but rotate on
 * every use.
 */
data class TokenProperties(
    @field:NotNull
    val accessTtl: Duration = Duration.ofMinutes(15),
    @field:NotNull
    val refreshTtl: Duration = Duration.ofDays(30),
) {
    init {
        if (accessTtl.isZero || accessTtl.isNegative) {
            throw IllegalStateException("app.security.tokens.access-ttl must be positive")
        }
        if (refreshTtl.isZero || refreshTtl.isNegative) {
            throw IllegalStateException("app.security.tokens.refresh-ttl must be positive")
        }
        if (refreshTtl < accessTtl) {
            throw IllegalStateException("app.security.tokens.refresh-ttl must not be shorter than access-ttl")
        }
    }
}
