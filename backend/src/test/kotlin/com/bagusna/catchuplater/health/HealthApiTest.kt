package com.bagusna.catchuplater.health

import com.bagusna.catchuplater.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class HealthApiTest : IntegrationTestBase() {

    @Test
    fun `health endpoint is public and reports up`() {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Request-ID"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("catch-up-later"))
    }

    @Test
    fun `error envelope carries the request id from the response header`() {
        val result = mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().exists("X-Request-ID"))
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.error.requestId", matchesPattern("^req_[A-Za-z0-9._-]+$")))
            .andReturn()

        val headerId = result.response.getHeader("X-Request-ID")
        val body = objectMapper.readTree(result.response.contentAsString)
        assertThat(body.get("error").get("requestId").asText()).isEqualTo(headerId)
    }

    @Test
    fun `client supplied request id is echoed back`() {
        mockMvc.perform(get("/api/v1/health").header("X-Request-ID", "req_client-supplied-123"))
            .andExpect(status().isOk)
            .andExpect(header().string("X-Request-ID", "req_client-supplied-123"))
    }
}
