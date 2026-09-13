package com.bagusna.catchuplater.core.security

import com.bagusna.catchuplater.features.auth.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DbUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmailIgnoreCase(username.trim())
            // The message is not surfaced to clients: DaoAuthenticationProvider
            // converts this into a generic BadCredentialsException.
            ?: throw UsernameNotFoundException("No account found for the provided credentials.")
        return AppUserPrincipal.from(user)
    }
}
