package com.bagusna.catchuplater.core.config

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.BearerTokenAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.DelegatingSecurityContextRepository
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.RequestMatcher

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    /**
     * Spring Security's recommended delegating encoder. New passwords are
     * encoded with BCrypt and stored with an `{id}` prefix, which allows a
     * future encoder migration without invalidating existing credentials.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): AuthenticationProvider =
        DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder)
        }

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    /**
     * Stores the security context in the HTTP session (and as a request
     * attribute for the duration of the request). Used both by Spring Security
     * and by the explicit login flow.
     */
    @Bean
    fun securityContextRepository(): SecurityContextRepository =
        DelegatingSecurityContextRepository(
            RequestAttributeSecurityContextRepository(),
            HttpSessionSecurityContextRepository(),
        )

    /**
     * Session fixation protection: the session id is changed on login.
     */
    @Bean
    fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy = ChangeSessionIdAuthenticationStrategy()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        properties: SecurityProperties,
        securityContextRepository: SecurityContextRepository,
        entryPoint: JsonAuthenticationEntryPoint,
        accessDeniedHandler: JsonAccessDeniedHandler,
        logoutSuccessHandler: JsonLogoutSuccessHandler,
        bearerTokenAuthenticationFilter: BearerTokenAuthenticationFilter,
    ): SecurityFilterChain {
        http
            // Cookie-based CSRF protection for the web client. Extension requests
            // are exempt: the token endpoints are public, and requests carrying a
            // bearer token have no session cookie to protect.
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                csrf.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                csrf.ignoringRequestMatchers("${ApiRoutes.V1}/auth/token", "${ApiRoutes.V1}/auth/token/**")
                csrf.ignoringRequestMatchers(
                    RequestMatcher { request ->
                        request.getHeader(BearerTokenAuthenticationFilter.AUTHORIZATION)
                            ?.startsWith(BearerTokenAuthenticationFilter.BEARER_PREFIX) == true
                    },
                )
            }
            .securityContext { it.securityContextRepository(securityContextRepository) }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                session.sessionFixation { it.changeSessionId() }
            }
            .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.GET, "${ApiRoutes.V1}/health").permitAll()
                auth.requestMatchers(HttpMethod.GET, "${ApiRoutes.V1}/auth/csrf").permitAll()
                auth.requestMatchers(HttpMethod.GET, "${ApiRoutes.V1}/setup/status").permitAll()
                auth.requestMatchers(
                    HttpMethod.POST,
                    "${ApiRoutes.V1}/auth/register",
                    "${ApiRoutes.V1}/auth/login",
                    "${ApiRoutes.V1}/setup",
                ).permitAll()
                auth.requestMatchers(
                    HttpMethod.POST,
                    "${ApiRoutes.V1}/auth/token",
                    "${ApiRoutes.V1}/auth/token/refresh",
                    "${ApiRoutes.V1}/auth/token/revoke",
                ).permitAll()
                auth.requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/scalar/**").permitAll()
                auth.requestMatchers("${ApiRoutes.V1}/admin/**").hasRole("ADMIN")
                auth.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(entryPoint)
                exceptions.accessDeniedHandler(accessDeniedHandler)
            }
            .logout { logout ->
                logout.logoutUrl("${ApiRoutes.V1}/auth/logout")
                logout.logoutSuccessHandler(logoutSuccessHandler)
                logout.invalidateHttpSession(true)
                logout.clearAuthentication(true)
                logout.deleteCookies(properties.session.cookieName)
            }
        return http.build()
    }
}
