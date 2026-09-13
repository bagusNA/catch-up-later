package com.bagusna.catchuplater.features.setup.service

import com.bagusna.catchuplater.common.error.SetupAlreadyCompletedException
import com.bagusna.catchuplater.features.auth.domain.RoleNames
import com.bagusna.catchuplater.features.auth.domain.User
import com.bagusna.catchuplater.features.auth.dto.RegistrationRequest
import com.bagusna.catchuplater.features.auth.repository.UserRepository
import com.bagusna.catchuplater.features.auth.service.UserRegistrationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * First-run bootstrap. Creates the only account (with both user and admin
 * roles) while the instance has no users. Open registration stays disabled.
 */
@Service
class SetupService(
    private val userRepository: UserRepository,
    private val userRegistrationService: UserRegistrationService,
) {
    @Transactional(readOnly = true)
    fun isSetupRequired(): Boolean = userRepository.count() == 0L

    @Transactional
    fun bootstrap(request: RegistrationRequest): User {
        if (userRepository.count() > 0L) {
            throw SetupAlreadyCompletedException()
        }
        return userRegistrationService.createUser(
            request = request,
            roleNames = setOf(RoleNames.USER, RoleNames.ADMIN),
        )
    }
}
