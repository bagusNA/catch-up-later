package com.bagusna.catchuplater.auth

import com.bagusna.catchuplater.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@Transactional
class AuthenticationApiTest : IntegrationTestBase() {

    @Test
    fun `logs in with valid credentials and exposes the current user`() {
        createUser("login@example.com")
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/login")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"login@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("login@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())

        // The security context persisted into the session for the next request.
        mockMvc.perform(get("/api/v1/auth/me").session(csrf.session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("login@example.com"))
    }

    @Test
    fun `rejects an invalid password with a generic message`() {
        createUser("bad-password@example.com")
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/login")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"bad-password@example.com","password":"definitely-not-the-password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.error.message").value("Invalid email or password."))
    }

    @Test
    fun `unknown email is indistinguishable from an invalid password`() {
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/login")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nobody@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.error.message").value("Invalid email or password."))
    }

    @Test
    fun `protected endpoint requires authentication`() {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects an invalid login payload`() {
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/login")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email","password":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `login creates a session when none exists yet`() {
        createUser("fresh-session@example.com")
        val csrf = csrfContext()

        // Send only the CSRF cookie/token, not a pre-existing HTTP session.
        val result = mockMvc.perform(
            post("/api/v1/auth/login")
                .cookie(csrf.cookie)
                .header("X-XSRF-TOKEN", csrf.token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"fresh-session@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isOk)
            .andReturn()

        assertThat(result.request.getSession(false)).isNotNull
    }

    @Test
    fun `logout invalidates the authenticated session`() {
        createUser("logout@example.com")
        val csrf = loginAs("logout@example.com")

        mockMvc.perform(get("/api/v1/auth/me").session(csrf.session))
            .andExpect(status().isOk)

        mockMvc.perform(post("/api/v1/auth/logout").withCsrf(csrf))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/auth/me").session(csrf.session))
            .andExpect(status().isUnauthorized)
    }
}
