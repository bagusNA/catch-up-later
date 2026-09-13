package com.bagusna.catchuplater.features.health

import com.bagusna.catchuplater.common.api.ApiRoutes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Liveness endpoint backing the foundation slice.
 *
 * Dependency-aware readiness (database and artifact storage) is added in the
 * operations slice.
 */
@RestController
@RequestMapping(ApiRoutes.V1)
class HealthController {

    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse(status = "UP", application = "catch-up-later")
}

data class HealthResponse(
    val status: String,
    val application: String,
)
