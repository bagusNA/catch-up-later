package com.bagusna.catchuplater.features.library.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.content.dto.PositionDto
import com.bagusna.catchuplater.features.content.dto.ReadingStateResponse
import com.bagusna.catchuplater.features.library.service.ReadingStateService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Owner-scoped reading progress. `PUT` also updates the item's `lastReadAt`.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/content-items/{id}/reading-state")
class ReadingStateController(
    private val readingStateService: ReadingStateService,
) {
    @GetMapping
    fun get(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): ReadingStateResponse = readingStateService.get(principal.id, id)

    @PutMapping
    fun update(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
        @RequestBody request: UpdateReadingStateRequest,
    ): ReadingStateResponse = readingStateService.update(
        ownerId = principal.id,
        contentItemId = id,
        status = request.status,
        progressPercent = request.progressPercent,
        position = request.position,
        markReadAtEnd = request.markRead == true,
    )
}

data class UpdateReadingStateRequest(
    val status: String? = null,
    val progressPercent: Double? = null,
    val position: PositionDto? = null,
    /** Explicit "finished" signal from the reader; sets status to READ. */
    val markRead: Boolean? = null,
)
