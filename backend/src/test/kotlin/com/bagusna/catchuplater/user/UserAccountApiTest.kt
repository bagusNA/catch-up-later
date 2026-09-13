package com.bagusna.catchuplater.user

import com.bagusna.catchuplater.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@Transactional
class UserAccountApiTest : IntegrationTestBase() {

    @Test
    fun `updates the display name of the authenticated account`() {
        createUser("profile@example.com")
        val csrf = loginAs("profile@example.com")

        mockMvc.perform(
            patch("/api/v1/users/me")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"New Name"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("profile@example.com"))
            .andExpect(jsonPath("$.displayName").value("New Name"))
    }

    @Test
    fun `rejects a password change with the wrong current password`() {
        createUser("wrong-current@example.com")
        val csrf = loginAs("wrong-current@example.com")

        mockMvc.perform(
            post("/api/v1/users/me/password")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"not-the-password","newPassword":"$VALID_PASSWORD"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"))
    }

    @Test
    fun `changes the password and hashes the new value`() {
        createUser("change@example.com")
        val csrf = loginAs("change@example.com")
        val newPassword = "a-brand-new-password"

        mockMvc.perform(
            post("/api/v1/users/me/password")
                .withCsrf(csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword":"$VALID_PASSWORD","newPassword":"$newPassword"}"""),
        ).andExpect(status().isNoContent)

        val stored = userRepository.findByEmailIgnoreCase("change@example.com")!!
        assertThat(passwordEncoder.matches(newPassword, stored.passwordHash)).isTrue()
        assertThat(passwordEncoder.matches(VALID_PASSWORD, stored.passwordHash)).isFalse()
    }
}
