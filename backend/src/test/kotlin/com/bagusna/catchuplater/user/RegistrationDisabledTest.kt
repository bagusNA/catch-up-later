package com.bagusna.catchuplater.user

import com.bagusna.catchuplater.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * Verifies that the public registration endpoint is closed when the feature is
 * disabled, which is the production default.
 */
@TestPropertySource(properties = ["app.security.registration.enabled=false"])
@Transactional
class RegistrationDisabledTest : IntegrationTestBase() {

    @Test
    fun `registration is rejected while disabled`() {
        val csrf = csrfContext()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"closed@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("REGISTRATION_DISABLED"))
    }
}
