package com.bagusna.catchuplater.features.auth.repository

import com.bagusna.catchuplater.features.auth.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Int> {
    fun findByEmailIgnoreCase(email: String): User?

    fun existsByEmailIgnoreCase(email: String): Boolean
}
