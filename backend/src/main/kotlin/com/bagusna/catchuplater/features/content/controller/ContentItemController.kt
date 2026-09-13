package com.bagusna.catchuplater.features.content.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.content.dto.ArtifactSummary
import com.bagusna.catchuplater.features.content.dto.ContentItemDetailResponse
import com.bagusna.catchuplater.features.content.dto.ContentItemListResponse
import com.bagusna.catchuplater.features.content.dto.ReaderResponse
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.service.ReaderService
import com.bagusna.catchuplater.features.content.service.toArtifactSummary
import com.bagusna.catchuplater.features.library.service.LibraryQueryService
import com.bagusna.catchuplater.features.library.service.LibraryService
import com.bagusna.catchuplater.features.library.service.TagService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Library listing, item detail, favorite/delete actions, and the article
 * reader payload. Every query is scoped to the authenticated owner.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/content-items")
class ContentItemController(
    private val libraryQueryService: LibraryQueryService,
    private val libraryService: LibraryService,
    private val readerService: ReaderService,
    private val artifacts: ArtifactVersionRepository,
    private val tagService: TagService,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) contentType: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) favorite: Boolean?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
    ): ContentItemListResponse = libraryQueryService.list(
        ownerId = principal.id,
        query = q,
        contentType = contentType,
        readingStatus = status,
        favorite = favorite,
        tag = tag,
        sort = sort,
        page = page,
        pageSize = pageSize,
    )

    @GetMapping("/{id}")
    fun detail(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): ContentItemDetailResponse = libraryService.detail(principal.id, id)

    @GetMapping("/{id}/artifacts")
    fun artifacts(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): List<ArtifactSummary> {
        libraryService.requireOwnedItem(principal.id, id)
        return artifacts.findByContentItemIdOrderByVersionNumberDesc(id).map { it.toArtifactSummary() }
    }

    @GetMapping("/{id}/reader")
    fun reader(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): ReaderResponse = readerService.reader(principal.id, id)

    @PostMapping("/{id}/favorite")
    fun favorite(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): FavoriteResponse = FavoriteResponse(libraryService.setFavorite(principal.id, id, true))

    @DeleteMapping("/{id}/favorite")
    fun unfavorite(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): FavoriteResponse = FavoriteResponse(libraryService.setFavorite(principal.id, id, false))

    @PutMapping("/{id}/tags")
    fun assignTags(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
        @RequestBody request: AssignTagsRequest,
    ): TagAssignmentResponse {
        tagService.assign(principal.id, id, request.tagIds)
        return TagAssignmentResponse(tagService.tagNamesFor(id, principal.id))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ) {
        libraryService.delete(principal.id, id)
    }
}

data class FavoriteResponse(val isFavorite: Boolean)

data class AssignTagsRequest(val tagIds: List<Int> = emptyList())

data class TagAssignmentResponse(val tags: List<String>)
