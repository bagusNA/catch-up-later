package com.bagusna.catchuplater.features.auth.service

import com.bagusna.catchuplater.common.error.InvalidTokenException
import com.bagusna.catchuplater.core.config.SecurityProperties
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.auth.domain.AuthToken
import com.bagusna.catchuplater.features.auth.domain.TokenType
import com.bagusna.catchuplater.features.auth.domain.User
import com.bagusna.catchuplater.features.auth.dto.TokenRefreshRequest
import com.bagusna.catchuplater.features.auth.dto.TokenRequest
import com.bagusna.catchuplater.features.auth.dto.TokenResponse
import com.bagusna.catchuplater.features.auth.dto.TokenRevokeRequest
import com.bagusna.catchuplater.features.auth.repository.AuthTokenRepository
import com.bagusna.catchuplater.features.auth.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Issues, rotates and validates opaque bearer tokens for the browser
 * extension.
 *
 * Only SHA-256 hashes are stored. Access and refresh tokens share a family id;
 * refreshing rotates the pair, and replaying a revoked refresh token revokes
 * the whole family.
 */
@Service
class TokenService(
    private val tokens: AuthTokenRepository,
    private val users: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val currentUserService: CurrentUserService,
    private val properties: SecurityProperties,
) {
    private val random = SecureRandom()

    @Transactional
    fun issueForCredentials(request: TokenRequest): TokenResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.email.trim(), request.password),
        )
        val principal = authentication.principal as AppUserPrincipal
        val user = users.findById(principal.id).orElseThrow { InvalidTokenException() }
        return issue(user)
    }

    @Transactional(noRollbackFor = [InvalidTokenException::class])
    fun refresh(request: TokenRefreshRequest): TokenResponse {
        val token = tokens.findByTokenHashAndTokenType(hash(request.refreshToken), TokenType.REFRESH)
            ?: throw InvalidTokenException()

        if (!token.isActive()) {
            // Replay of an expired or already-rotated refresh token: revoke
            // every token in the family.
            tokens.revokeFamily(token.familyId)
            throw InvalidTokenException()
        }

        token.revoked = true
        tokens.save(token)

        val user = users.findById(token.userId).orElse(null)
        if (user == null || !user.enabled) {
            tokens.revokeFamily(token.familyId)
            throw InvalidTokenException()
        }

        return issue(user, token.familyId)
    }

    @Transactional
    fun revoke(request: TokenRevokeRequest) {
        val token = tokens.findByTokenHash(hash(request.refreshToken)) ?: return
        tokens.revokeFamily(token.familyId)
    }

    @Transactional
    fun revokeAllForUser(userId: Int) {
        tokens.revokeAllForUser(userId)
    }

    @Transactional(readOnly = true)
    fun authenticateAccess(accessToken: String): AppUserPrincipal? {
        if (accessToken.isBlank()) return null
        val token = tokens.findByTokenHashAndTokenType(hash(accessToken), TokenType.ACCESS) ?: return null
        if (!token.isActive()) return null
        val user = users.findById(token.userId).orElse(null) ?: return null
        if (!user.enabled) return null
        return AppUserPrincipal.from(user)
    }

    private fun issue(user: User, familyId: String = UUID.randomUUID().toString()): TokenResponse {
        val userId = requireNotNull(user.id) { "User must be persisted before issuing tokens" }
        val now = Instant.now()
        val accessToken = randomToken()
        val refreshToken = randomToken()

        tokens.saveAll(
            listOf(
                AuthToken(
                    userId = userId,
                    tokenHash = hash(accessToken),
                    tokenType = TokenType.ACCESS,
                    familyId = familyId,
                    expiresAt = now.plus(properties.tokens.accessTtl),
                ),
                AuthToken(
                    userId = userId,
                    tokenHash = hash(refreshToken),
                    tokenType = TokenType.REFRESH,
                    familyId = familyId,
                    expiresAt = now.plus(properties.tokens.refreshTtl),
                ),
            ),
        )

        return TokenResponse(
            accessToken = accessToken,
            expiresIn = properties.tokens.accessTtl.seconds,
            refreshToken = refreshToken,
            refreshExpiresIn = properties.tokens.refreshTtl.seconds,
            user = currentUserService.toResponse(user),
        )
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
