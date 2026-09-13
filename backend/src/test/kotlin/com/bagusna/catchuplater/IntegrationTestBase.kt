package com.bagusna.catchuplater

import com.bagusna.catchuplater.features.auth.domain.RoleNames
import com.bagusna.catchuplater.features.auth.domain.User
import com.bagusna.catchuplater.features.auth.repository.AuthTokenRepository
import com.bagusna.catchuplater.features.auth.repository.RoleRepository
import com.bagusna.catchuplater.features.auth.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Shared integration test setup. Every test context runs Flyway migrations
 * against a real (file-based) SQLite database, so migrations and constraints
 * are exercised exactly as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var roleRepository: RoleRepository

    @Autowired
    protected lateinit var authTokenRepository: AuthTokenRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    protected data class CsrfContext(
        val session: MockHttpSession,
        val cookie: Cookie,
        val token: String,
    )

    /**
     * Removes all accounts and tokens. Used by token/setup tests that must
     * observe committed state across request boundaries (and therefore cannot
     * run inside a single rolled-back transaction).
     */
    protected fun clearAccounts() {
        authTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    /**
     * Bootstraps a real CSRF token through the API the way a browser client
     * would: fetch it, keep the cookie and send the token back in the header.
     */
    protected fun csrfContext(session: MockHttpSession = MockHttpSession()): CsrfContext {
        val result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
            .andExpect(status().isOk)
            .andReturn()
        val token = objectMapper.readTree(result.response.contentAsString).get("token").asText()
        val cookie = result.response.getCookie("XSRF-TOKEN")
            ?: error("Expected an XSRF-TOKEN cookie to be set")
        return CsrfContext(session, cookie, token)
    }

    protected fun MockHttpServletRequestBuilder.withCsrf(context: CsrfContext): MockHttpServletRequestBuilder =
        session(context.session)
            .cookie(context.cookie)
            .header("X-XSRF-TOKEN", context.token)

    /**
     * Logs in through the real API and returns a session with an authenticated
     * security context.
     */
    protected fun loginAs(email: String, password: String = VALID_PASSWORD): CsrfContext {
        val context = csrfContext()
        mockMvc.perform(
            post("/api/v1/auth/login")
                .withCsrf(context)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}"""),
        ).andExpect(status().isOk)
        return context
    }

    /**
     * Creates a persisted user directly so authentication/authorization tests
     * do not depend on the registration endpoint.
     */
    protected fun createUser(
        email: String,
        password: String = VALID_PASSWORD,
        roleNames: Set<String> = setOf(RoleNames.USER),
    ): User {
        val roles = roleNames.map { name ->
            roleRepository.findByName(name) ?: error("Required role $name is missing")
        }.toMutableSet()
        val user = User(
            email = email.trim().lowercase(),
            passwordHash = passwordEncoder.encode(password)!!,
            displayName = null,
            roles = roles,
        )
        return userRepository.saveAndFlush(user)
    }

    companion object {
        const val VALID_PASSWORD = "correct-horse-battery-staple"

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            val directory = Path.of("build", "test-data").toAbsolutePath()
            Files.createDirectories(directory)
            val database = Files.createTempFile(directory, "catch-up-later-test-", ".db")
            registry.add("spring.datasource.url") { "jdbc:sqlite:${database.toAbsolutePath()}" }

            // Each test context gets isolated artifact storage so a fresh
            // database cannot collide with files left by a previous run.
            val storage = Files.createTempDirectory(directory, "catch-up-later-storage-")
            registry.add("app.storage.artifacts-dir") { storage.resolve("artifacts").toString() }
            registry.add("app.storage.staging-dir") { storage.resolve("staging").toString() }
        }
    }
}
