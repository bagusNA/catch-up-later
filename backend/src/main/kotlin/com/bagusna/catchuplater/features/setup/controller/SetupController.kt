package com.bagusna.catchuplater.features.setup.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.features.auth.dto.CurrentUserResponse
import com.bagusna.catchuplater.features.auth.dto.RegistrationRequest
import com.bagusna.catchuplater.features.auth.service.CurrentUserService
import com.bagusna.catchuplater.features.setup.dto.SetupStatusResponse
import com.bagusna.catchuplater.features.setup.service.SetupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * First-run bootstrap endpoints. Both are public, but `POST /setup` only
 * succeeds while the instance has no accounts.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/setup")
class SetupController(
    private val setupService: SetupService,
    private val currentUserService: CurrentUserService,
) {
    @GetMapping("/status")
    fun status(): SetupStatusResponse = SetupStatusResponse(required = setupService.isSetupRequired())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun setup(@Valid @RequestBody request: RegistrationRequest): CurrentUserResponse =
        currentUserService.toResponse(setupService.bootstrap(request))
}
