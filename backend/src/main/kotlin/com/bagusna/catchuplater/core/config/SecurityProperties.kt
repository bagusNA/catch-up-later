package com.bagusna.catchuplater.core.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.server.Cookie
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Application security settings. Invalid values fail fast during startup
 * because the class is validated when bound.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
data class SecurityProperties(
    @field:Valid
    val session: SessionProperties = SessionProperties(),
    @field:Valid
    val registration: RegistrationProperties = RegistrationProperties(),
) {
    data class SessionProperties(
        @field:NotBlank
        val cookieName: String = "CUL_SESSION",
        /**
         * Must be true outside local HTTP development. `SameSite=None` requires
         * a secure cookie.
         */
        val cookieSecure: Boolean = true,
        @field:NotBlank
        val cookieSameSite: String = "Lax",
        @field:NotNull
        val timeout: Duration = Duration.ofMinutes(30),
    ) {
        init {
            val sameSite = sameSite()
            if (sameSite == Cookie.SameSite.NONE && !cookieSecure) {
                throw IllegalStateException(
                    "app.security.session.cookie-same-site=None requires app.security.session.cookie-secure=true",
                )
            }
            if (timeout.isZero || timeout.isNegative) {
                throw IllegalStateException("app.security.session.timeout must be positive")
            }
        }

        fun sameSite(): Cookie.SameSite = when (cookieSameSite.trim().lowercase()) {
            "lax" -> Cookie.SameSite.LAX
            "strict" -> Cookie.SameSite.STRICT
            "none" -> Cookie.SameSite.NONE
            "omitted" -> Cookie.SameSite.OMITTED
            else -> throw IllegalStateException(
                "app.security.session.cookie-same-site must be one of Lax, Strict, None, Omitted but was '$cookieSameSite'",
            )
        }
    }

    data class RegistrationProperties(
        val enabled: Boolean = true,
        @field:Min(8)
        @field:Max(256)
        val minPasswordLength: Int = 12,
        @field:Min(8)
        @field:Max(256)
        val maxPasswordLength: Int = 128,
    ) {
        init {
            if (maxPasswordLength < minPasswordLength) {
                throw IllegalStateException(
                    "app.security.registration.max-password-length must be greater than or equal to min-password-length",
                )
            }
        }
    }
}
