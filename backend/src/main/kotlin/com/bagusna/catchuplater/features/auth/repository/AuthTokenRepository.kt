package com.bagusna.catchuplater.features.auth.repository

import com.bagusna.catchuplater.features.auth.domain.AuthToken
import com.bagusna.catchuplater.features.auth.domain.TokenType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AuthTokenRepository : JpaRepository<AuthToken, Int> {

    fun findByTokenHash(tokenHash: String): AuthToken?

    fun findByTokenHashAndTokenType(tokenHash: String, tokenType: TokenType): AuthToken?

    @Modifying
    @Query("update AuthToken t set t.revoked = true where t.familyId = :familyId and t.revoked = false")
    fun revokeFamily(familyId: String): Int

    @Modifying
    @Query("update AuthToken t set t.revoked = true where t.userId = :userId and t.revoked = false")
    fun revokeAllForUser(userId: Int): Int

    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :cutoff")
    fun deleteExpiredBefore(cutoff: java.time.Instant): Int
}
