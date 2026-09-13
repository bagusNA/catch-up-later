package com.bagusna.catchuplater.user

import com.bagusna.catchuplater.IntegrationTestBase
import com.bagusna.catchuplater.features.auth.dto.RegistrationRequest
import com.bagusna.catchuplater.features.auth.service.UserRegistrationService
import com.bagusna.catchuplater.features.auth.service.isUniqueConstraintViolation
import com.bagusna.catchuplater.features.auth.domain.RoleNames
import com.bagusna.catchuplater.features.auth.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException
import java.time.Instant

@Transactional
class UserPersistenceTest : IntegrationTestBase() {

    @Autowired
    private lateinit var registrationService: UserRegistrationService

    @Test
    fun `database enforces a case-insensitive unique email`() {
        createUser("unique@example.com")
        val role = roleRepository.findByName(RoleNames.USER) ?: error("role missing")

        assertThatThrownBy {
            userRepository.saveAndFlush(
                User(
                    email = "UNIQUE@example.com",
                    passwordHash = "irrelevant-hash",
                    roles = mutableSetOf(role),
                ),
            )
        }
            .isInstanceOf(DataAccessException::class.java)
            .hasMessageContaining("UNIQUE constraint failed")
    }

    @Test
    fun `detects unique constraint violations in a nested cause chain`() {
        val sqlException = SQLException("constraint", "23000")
        val wrapped = RuntimeException("wrapper", sqlException)
        assertThat(isUniqueConstraintViolation(wrapped)).isTrue()
        assertThat(isUniqueConstraintViolation(RuntimeException("unrelated"))).isFalse()
    }

    @Test
    fun `persists roles and audit timestamps`() {
        createUser("persisted@example.com", roleNames = setOf(RoleNames.USER, RoleNames.ADMIN))

        val reloaded = userRepository.findByEmailIgnoreCase("persisted@example.com")
        assertThat(reloaded).isNotNull
        assertThat(reloaded!!.roles.map { it.name })
            .containsExactlyInAnyOrder(RoleNames.USER, RoleNames.ADMIN)
        assertThat(reloaded.createdAt).isAfter(Instant.EPOCH)
        assertThat(reloaded.updatedAt).isAfter(Instant.EPOCH)
        assertThat(reloaded.enabled).isTrue()
    }

    @Test
    fun `registration stores a password hash and never the plaintext`() {
        val user = registrationService.register(
            RegistrationRequest(
                email = "hashed@example.com",
                password = VALID_PASSWORD,
                displayName = null,
            ),
        )

        assertThat(user.passwordHash).startsWith("{bcrypt}")
        assertThat(user.passwordHash).doesNotContain(VALID_PASSWORD)
        assertThat(passwordEncoder.matches(VALID_PASSWORD, user.passwordHash)).isTrue()

        val reloaded = userRepository.findByEmailIgnoreCase("hashed@example.com")
        assertThat(reloaded!!.passwordHash).isEqualTo(user.passwordHash)
    }
}
