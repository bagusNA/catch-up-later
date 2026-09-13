package com.bagusna.catchuplater.features.user.service

import com.bagusna.catchuplater.common.error.InvalidPasswordException
import com.bagusna.catchuplater.common.error.ResourceNotFoundException
import com.bagusna.catchuplater.common.error.WeakPasswordException
import com.bagusna.catchuplater.core.config.SecurityProperties
import com.bagusna.catchuplater.features.auth.domain.User
import com.bagusna.catchuplater.features.auth.repository.UserRepository
import com.bagusna.catchuplater.features.auth.service.TokenService
import com.bagusna.catchuplater.features.user.dto.ChangePasswordRequest
import com.bagusna.catchuplater.features.user.dto.UpdateProfileRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAccountService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val properties: SecurityProperties,
    private val tokenService: TokenService,
) {
    @Transactional
    fun updateProfile(userId: Int, request: UpdateProfileRequest): User {
        val user = users.findById(userId).orElseThrow { ResourceNotFoundException("Account not found.") }
        user.displayName = request.displayName?.trim()?.takeIf { it.isNotEmpty() }
        return users.saveAndFlush(user)
    }

    @Transactional
    fun changePassword(userId: Int, request: ChangePasswordRequest) {
        val user = users.findById(userId).orElseThrow { ResourceNotFoundException("Account not found.") }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw InvalidPasswordException()
        }

        val min = properties.registration.minPasswordLength
        val max = properties.registration.maxPasswordLength
        if (request.newPassword.length < min || request.newPassword.length > max) {
            throw WeakPasswordException("Password must be between $min and $max characters long.")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
            ?: throw IllegalStateException("Password encoder returned no value")
        users.saveAndFlush(user)

        // A password change invalidates every existing extension token.
        tokenService.revokeAllForUser(userId)
    }
}
