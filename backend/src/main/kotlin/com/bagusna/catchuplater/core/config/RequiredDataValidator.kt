package com.bagusna.catchuplater.core.config

import com.bagusna.catchuplater.features.auth.domain.RoleNames
import com.bagusna.catchuplater.features.auth.repository.RoleRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Fails fast if the roles the application depends on are missing, which would
 * indicate that migrations did not run or were modified incompatibly.
 */
@Component
class RequiredDataValidator(
    private val roleRepository: RoleRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val requiredRoles = listOf(RoleNames.USER, RoleNames.ADMIN)
        val missing = requiredRoles.filter { roleRepository.findByName(it) == null }
        check(missing.isEmpty()) {
            "Missing required roles $missing. Verify that Flyway migrations ran successfully."
        }
    }
}
