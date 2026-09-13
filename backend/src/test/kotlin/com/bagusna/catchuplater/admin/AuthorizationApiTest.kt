package com.bagusna.catchuplater.admin

import com.bagusna.catchuplater.IntegrationTestBase
import com.bagusna.catchuplater.features.auth.domain.RoleNames
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@Transactional
class AuthorizationApiTest : IntegrationTestBase() {

    @Test
    fun `regular user is forbidden from the admin endpoint`() {
        createUser("regular@example.com", roleNames = setOf(RoleNames.USER))
        val csrf = loginAs("regular@example.com")

        mockMvc.perform(get("/api/admin/overview").session(csrf.session))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
    }

    @Test
    fun `admin can access the admin endpoint`() {
        createUser("admin@example.com", roleNames = setOf(RoleNames.USER, RoleNames.ADMIN))
        val csrf = loginAs("admin@example.com")

        mockMvc.perform(get("/api/admin/overview").session(csrf.session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.adminEmail").value("admin@example.com"))
    }

    @Test
    fun `anonymous user is challenged for the admin endpoint`() {
        mockMvc.perform(get("/api/admin/overview"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }
}
