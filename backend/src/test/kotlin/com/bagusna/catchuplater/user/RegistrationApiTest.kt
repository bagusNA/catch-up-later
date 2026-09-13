package com.bagusna.catchuplater.user

import com.bagusna.catchuplater.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@Transactional
class RegistrationApiTest : IntegrationTestBase() {

    @Test
    fun `registers an account and returns only public data`() {
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "New.User@Example.com",
                      "password": "$VALID_PASSWORD",
                      "displayName": "New User"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.email").value("new.user@example.com"))
            .andExpect(jsonPath("$.displayName").value("New User"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())

        val stored = userRepository.findByEmailIgnoreCase("new.user@example.com")
        assertThat(stored).isNotNull
        assertThat(stored!!.passwordHash).isNotEqualTo(VALID_PASSWORD)
        assertThat(passwordEncoder.matches(VALID_PASSWORD, stored.passwordHash)).isTrue()
    }

    @Test
    fun `rejects a duplicate email without leaking internals`() {
        createUser("duplicate@example.com")
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "duplicate@example.com",
                      "password": "$VALID_PASSWORD"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"))
            .andExpect(jsonPath("$.error.message").value("An account with this email already exists."))
    }

    @Test
    fun `rejects a duplicate email with different casing`() {
        createUser("case@example.com")
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"CASE@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"))
    }

    @Test
    fun `returns field level validation errors for an invalid payload`() {
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.error.details.fieldErrors[0].field").value("email"))
    }

    @Test
    fun `enforces the configured password length`() {
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"weak@example.com","password":"short"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("WEAK_PASSWORD"))
    }

    @Test
    fun `rejects a state changing request without a csrf token`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nocsrf@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isForbidden)
    }
}
