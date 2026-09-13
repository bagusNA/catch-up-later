package com.bagusna.catchuplater.core.config

import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Applies the configured session cookie attributes. Boot's own customizer runs
 * first (order 0) and this one runs last, so these values win.
 */
@Configuration
class SessionCookieConfiguration(
    private val properties: SecurityProperties,
) {
    @Bean
    fun sessionCookieCustomizer(): WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> =
        WebServerFactoryCustomizer { factory ->
            val settings = factory.settings
            val session = settings.session
            session.timeout = properties.session.timeout
            session.cookie.name = properties.session.cookieName
            session.cookie.httpOnly = true
            session.cookie.secure = properties.session.cookieSecure
            session.cookie.sameSite = properties.session.sameSite()
        }
}
