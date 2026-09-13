package com.bagusna.catchuplater.config

import com.bagusna.catchuplater.features.auth.dto.RegistrationRequest
import com.bagusna.catchuplater.features.auth.service.UserRegistrationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.ObjectMapper
import java.net.CookieManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path

/**
 * Boots the real servlet container to verify that the configured session cookie
 * is actually applied to responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionCookieIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var registrationService: UserRegistrationService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `login issues a hardened session cookie with the configured name`() {
        registrationService.register(
            RegistrationRequest("cookie@example.com", VALID_PASSWORD, null),
        )

        val cookieManager = CookieManager()
        val client = HttpClient.newBuilder().cookieHandler(cookieManager).build()

        val csrfResponse = client.send(
            HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        val token = objectMapper.readTree(csrfResponse.body()).get("token").asText()

        val loginResponse = client.send(
            HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("X-XSRF-TOKEN", token)
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        """{"email":"cookie@example.com","password":"$VALID_PASSWORD"}""",
                    ),
                )
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

        assertThat(loginResponse.statusCode()).isEqualTo(200)
        val sessionCookie = loginResponse.headers().allValues("set-cookie")
            .firstOrNull { it.startsWith("CUL_SESSION=") }
        assertThat(sessionCookie).isNotNull
        assertThat(sessionCookie!!.lowercase())
            .contains("httponly")
            .contains("samesite=lax")
    }

    private fun uri(path: String): URI = URI.create("http://localhost:$port$path")

    companion object {
        const val VALID_PASSWORD = "correct-horse-battery-staple"

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            val directory = Path.of("build", "test-data").toAbsolutePath()
            Files.createDirectories(directory)
            val database = Files.createTempFile(directory, "catch-up-later-web-", ".db")
            registry.add("spring.datasource.url") { "jdbc:sqlite:${database.toAbsolutePath()}" }
        }
    }
}
