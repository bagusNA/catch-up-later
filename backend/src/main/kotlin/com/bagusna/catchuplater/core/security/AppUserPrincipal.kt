package com.bagusna.catchuplater.core.security

import com.bagusna.catchuplater.features.auth.domain.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Spring Security principal backed by an application [User]. Exposes only the
 * data needed for authentication and authorization.
 */
class AppUserPrincipal(
    val id: Int,
    val email: String,
    val displayName: String?,
    private val passwordHash: String,
    private val accountEnabled: Boolean,
    private val grantedAuthorities: Set<GrantedAuthority>,
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = grantedAuthorities

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = accountEnabled

    companion object {
        fun from(user: User): AppUserPrincipal {
            val id = requireNotNull(user.id) { "User must be persisted before building a principal" }
            return AppUserPrincipal(
                id = id,
                email = user.email,
                displayName = user.displayName,
                passwordHash = user.passwordHash,
                accountEnabled = user.enabled,
                grantedAuthorities = user.roles.map { SimpleGrantedAuthority(it.name) }.toSet(),
            )
        }
    }
}
