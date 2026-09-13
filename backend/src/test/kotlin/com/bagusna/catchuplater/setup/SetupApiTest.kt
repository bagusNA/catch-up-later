package com.bagusna.catchuplater.setup

import com.bagusna.catchuplater.IntegrationTestBase
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * First-run bootstrap. These tests are deliberately not transactional so the
 * service observes state committed by previous HTTP requests, exactly as it
 * would in production.
 */
class SetupApiTest : IntegrationTestBase() {

    @AfterEach
    fun cleanup() = clearAccounts()

    @Test
    fun `requires setup on an empty instance`() {
        mockMvc.perform(get("/api/v1/setup/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.required").value(true))
    }

    @Test
    fun `creates the first account as an admin and then closes setup`() {
        val csrf = csrfContext()
        mockMvc.perform(
            post("/api/v1/setup")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email":"owner@example.com","password":"$VALID_PASSWORD","displayName":"Owner"}""",
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")))
            .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")))

        mockMvc.perform(get("/api/v1/setup/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.required").value(false))
    }

    @Test
    fun `rejects setup once an account exists`() {
        val first = csrfContext()
        mockMvc.perform(
            post("/api/v1/setup")
                .withCsrf(first)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"first@example.com","password":"$VALID_PASSWORD"}"""),
        ).andExpect(status().isCreated)

        val second = csrfContext()
        mockMvc.perform(
            post("/api/v1/setup")
                .withCsrf(second)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"second@example.com","password":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("SETUP_ALREADY_COMPLETED"))
    }
}
