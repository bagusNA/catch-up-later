package com.bagusna.catchuplater.features.auth.service

import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.auth.dto.CurrentUserResponse
import com.bagusna.catchuplater.features.auth.domain.User
import org.springframework.stereotype.Service

@Service
class CurrentUserService {

    fun toResponse(principal: AppUserPrincipal): CurrentUserResponse =
        CurrentUserResponse(
            id = principal.id,
            email = principal.email,
            displayName = principal.displayName,
            roles = principal.authorities.mapNotNull { it.authority }.sorted(),
        )

    fun toResponse(user: User): CurrentUserResponse =
        CurrentUserResponse(
            id = requireNotNull(user.id) { "User must be persisted before building a response" },
            email = user.email,
            displayName = user.displayName,
            roles = user.roles.map { it.name }.sorted(),
        )
}
