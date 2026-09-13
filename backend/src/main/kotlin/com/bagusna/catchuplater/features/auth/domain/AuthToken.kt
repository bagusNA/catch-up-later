package com.bagusna.catchuplater.features.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant

/**
 * An opaque bearer token. Only the SHA-256 hash of the token is persisted.
 *
 * Access and refresh tokens belong to a *family*. When a refresh token is used
 * it is revoked and a new pair is issued in the same family. Presenting an
 * already-revoked refresh token revokes the entire family (replay detection).
 */
@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Int = 0,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 16)
    var tokenType: TokenType = TokenType.ACCESS,

    @Column(name = "family_id", nullable = false, length = 36)
    var familyId: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.EPOCH,

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }

    fun isActive(now: Instant = Instant.now()): Boolean = !revoked && expiresAt.isAfter(now)
}

enum class TokenType {
    ACCESS,
    REFRESH,
}
