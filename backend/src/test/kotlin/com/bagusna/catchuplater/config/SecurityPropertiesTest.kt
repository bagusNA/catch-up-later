package com.bagusna.catchuplater.config

import com.bagusna.catchuplater.core.config.SecurityProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.web.server.Cookie
import java.time.Duration

class SecurityPropertiesTest {

    @Test
    fun `defaults are secure`() {
        val properties = SecurityProperties()
        assertThat(properties.session.cookieName).isEqualTo("CUL_SESSION")
        assertThat(properties.session.cookieSecure).isTrue()
        assertThat(properties.session.sameSite()).isEqualTo(Cookie.SameSite.LAX)
        assertThat(properties.registration.minPasswordLength).isEqualTo(12)
        assertThat(properties.registration.enabled).isFalse()
        assertThat(properties.tokens.accessTtl).isEqualTo(Duration.ofMinutes(15))
        assertThat(properties.tokens.refreshTtl).isEqualTo(Duration.ofDays(30))
    }

    @Test
    fun `same-site none requires a secure cookie`() {
        assertThatThrownBy {
            SecurityProperties(
                session = SecurityProperties.SessionProperties(
                    cookieSecure = false,
                    cookieSameSite = "None",
                ),
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `rejects an unknown same-site value`() {
        assertThatThrownBy {
            SecurityProperties(
                session = SecurityProperties.SessionProperties(cookieSameSite = "Sometimes"),
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `rejects a non-positive session timeout`() {
        assertThatThrownBy {
            SecurityProperties(
                session = SecurityProperties.SessionProperties(timeout = Duration.ZERO),
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `rejects inverted password length bounds`() {
        assertThatThrownBy {
            SecurityProperties(
                registration = SecurityProperties.RegistrationProperties(
                    minPasswordLength = 20,
                    maxPasswordLength = 10,
                ),
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }
}
