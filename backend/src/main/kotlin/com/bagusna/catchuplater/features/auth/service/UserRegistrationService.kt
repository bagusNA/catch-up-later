package com.bagusna.catchuplater.features.auth.service

import com.bagusna.catchuplater.common.error.DuplicateEmailException
import com.bagusna.catchuplater.common.error.RegistrationDisabledException
import com.bagusna.catchuplater.common.error.WeakPasswordException
import com.bagusna.catchuplater.core.config.SecurityProperties
import com.bagusna.catchuplater.features.auth.dto.RegistrationRequest
import com.bagusna.catchuplater.features.auth.domain.RoleNames
import com.bagusna.catchuplater.features.auth.domain.User
import com.bagusna.catchuplater.features.auth.repository.RoleRepository
import com.bagusna.catchuplater.features.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val properties: SecurityProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Registers a new account through the public registration endpoint. The
     * endpoint is disabled by default and only available when explicitly
     * enabled; first-run bootstrap uses [createUser] directly.
     */
    @Transactional
    fun register(request: RegistrationRequest): User {
        if (!properties.registration.enabled) {
            throw RegistrationDisabledException()
        }
        return createUser(request, setOf(RoleNames.USER))
    }

    /**
     * Creates an account with the given roles. The transaction boundary covers
     * the uniqueness check, role assignment and insert so the database unique
     * constraint is the final authority on duplicates.
     */
    @Transactional
    fun createUser(request: RegistrationRequest, roleNames: Set<String>): User {
        val email = normalizeEmail(request.email)
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateEmailException()
        }
        enforcePasswordPolicy(request.password)

        val roles = roleNames.map { name ->
            roleRepository.findByName(name)
                ?: throw IllegalStateException("Required role $name is not configured")
        }

        val user = User(
            email = email,
            passwordHash = passwordEncoder.encode(request.password)
                ?: throw IllegalStateException("Password encoder returned no value"),
            displayName = request.displayName?.trim()?.takeIf { it.isNotEmpty() },
        )
        roles.forEach { user.addRole(it) }

        return try {
            // Flush so the unique constraint is evaluated inside this
            // transaction and can be translated into a domain error.
            userRepository.saveAndFlush(user)
        } catch (exception: DataAccessException) {
            // SQLite's dialect does not translate unique violations into
            // DataIntegrityViolationException, so inspect the cause chain.
            if (isUniqueConstraintViolation(exception)) {
                log.debug("Registration rejected by the unique email constraint", exception)
                throw DuplicateEmailException()
            }
            throw exception
        }
    }

    private fun enforcePasswordPolicy(password: String) {
        val min = properties.registration.minPasswordLength
        val max = properties.registration.maxPasswordLength
        if (password.length < min || password.length > max) {
            throw WeakPasswordException("Password must be between $min and $max characters long.")
        }
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()
}

/**
 * True when the exception chain represents a database uniqueness violation.
 * Checks both the SQL state class (23xxx = integrity constraint violation) and
 * the common SQLite message text so the behaviour is driver-independent.
 */
internal fun isUniqueConstraintViolation(exception: Throwable): Boolean {
    var current: Throwable? = exception
    while (current != null) {
        val sqlState = (current as? SQLException)?.sqlState
        if (sqlState != null && sqlState.startsWith("23")) {
            return true
        }
        val message = current.message
        if (message != null && message.contains("unique constraint failed", ignoreCase = true)) {
            return true
        }
        current = current.cause
    }
    return false
}
