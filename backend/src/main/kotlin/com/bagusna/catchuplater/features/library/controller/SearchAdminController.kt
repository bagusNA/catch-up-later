package com.bagusna.catchuplater.features.library.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.common.error.ResourceNotFoundException
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.library.service.SearchIndexService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Administrative FTS5 maintenance. Guarded by the admin role rule in
 * `SecurityConfig` and additionally annotated for clarity.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/admin/search")
@PreAuthorize("hasRole('ADMIN')")
class SearchAdminController(
    private val searchIndex: SearchIndexService,
) {
    @PostMapping("/reindex")
    fun reindex(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @RequestParam(required = false) contentItemId: Int?,
    ): ReindexResponse {
        if (contentItemId == null) {
            val count = searchIndex.reindexAll(principal.id)
            return ReindexResponse(reindexed = count, scope = "all")
        }
        val found = searchIndex.reindexItem(principal.id, contentItemId)
        if (!found) throw ResourceNotFoundException("The content item was not found.")
        return ReindexResponse(reindexed = 1, scope = "item", contentItemId = contentItemId)
    }

    @GetMapping("/integrity")
    fun integrity(@AuthenticationPrincipal principal: AppUserPrincipal): SearchIndexService.IndexIntegrity =
        searchIndex.integrity(principal.id)
}

data class ReindexResponse(
    val reindexed: Int,
    val scope: String,
    val contentItemId: Int? = null,
)
