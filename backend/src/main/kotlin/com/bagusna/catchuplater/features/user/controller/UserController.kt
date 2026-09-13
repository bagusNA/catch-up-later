package com.bagusna.catchuplater.features.user.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.auth.dto.CurrentUserResponse
import com.bagusna.catchuplater.features.auth.service.CurrentUserService
import com.bagusna.catchuplater.features.user.dto.ChangePasswordRequest
import com.bagusna.catchuplater.features.user.dto.UpdateProfileRequest
import com.bagusna.catchuplater.features.user.service.UserAccountService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Account management for the authenticated user. Works for both session and
 * bearer authentication because both resolve to an [AppUserPrincipal].
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/users")
class UserController(
    private val userAccountService: UserAccountService,
    private val currentUserService: CurrentUserService,
) {
    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): CurrentUserResponse =
        currentUserService.toResponse(userAccountService.updateProfile(principal.id, request))

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest,
    ) {
        userAccountService.changePassword(principal.id, request)
    }
}
