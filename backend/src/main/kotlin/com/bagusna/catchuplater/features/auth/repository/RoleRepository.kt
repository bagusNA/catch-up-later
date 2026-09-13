package com.bagusna.catchuplater.features.auth.repository

import com.bagusna.catchuplater.features.auth.domain.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Int> {
    fun findByName(name: String): Role?
}
