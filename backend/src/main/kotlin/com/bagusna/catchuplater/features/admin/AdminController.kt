package com.bagusna.catchuplater.features.admin

import com.bagusna.catchuplater.features.admin.dto.AdminOverviewResponse
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin-only example endpoint. Access is enforced both by the URL rule for the
 * admin path (ROLE_ADMIN required) and by method security.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController {

    @GetMapping("/overview")
    fun overview(@AuthenticationPrincipal principal: AppUserPrincipal): AdminOverviewResponse =
        AdminOverviewResponse(
            message = "Admin access granted.",
            adminEmail = principal.email,
        )
}
