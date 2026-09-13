package com.bagusna.catchuplater.auth

import com.bagusna.catchuplater.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Bearer token flows for the extension. Not transactional: token rotation and
 * reuse detection depend on committed state between requests.
 */
class TokenApiTest : IntegrationTestBase() {

    @AfterEach
    fun cleanup() = clearAccounts()

    private fun issueTokens(email: String = "token@example.com"): Pair<String, String> {
        createUser(email)
        val result = mockMvc.perform(
            post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value(email))
            .andReturn()
        val body = objectMapper.readTree(result.response.contentAsString)
        return body.get("accessToken").asText() to body.get("refreshToken").asText()
    }

    @Test
    fun `issues tokens without a csrf token and authenticates subsequent requests`() {
        val (access, _) = issueTokens()

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $access"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("token@example.com"))
    }

    @Test
    fun `refresh rotates the pair and replaying the old token revokes the family`() {
        val (_, refresh) = issueTokens("rotate@example.com")

        val refreshed = mockMvc.perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refresh"}"""),
        )
            .andExpect(status().isOk)
            .andReturn()
        val newRefresh = objectMapper.readTree(refreshed.response.contentAsString).get("refreshToken").asText()
        assertThat(newRefresh).isNotEqualTo(refresh)

        mockMvc.perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refresh"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"))

        // Replay detection revokes the freshly issued refresh token as well.
        mockMvc.perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$newRefresh"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `revoke invalidates the refresh token`() {
        val (_, refresh) = issueTokens("revoke@example.com")

        mockMvc.perform(
            post("/api/v1/auth/token/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refresh"}"""),
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refresh"}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `rejects an invalid access token`() {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects wrong credentials`() {
        createUser("wrong@example.com")

        mockMvc.perform(
            post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"wrong@example.com","password":"definitely-not-it"}"""),
        ).andExpect(status().isUnauthorized)
    }
}
