package com.bagusna.catchuplater.features.content.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.content.dto.ContentItemListResponse
import com.bagusna.catchuplater.features.content.dto.ReaderResponse
import com.bagusna.catchuplater.features.content.service.ContentItemService
import com.bagusna.catchuplater.features.content.service.ReaderService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Library listing and the article reader payload. Every query is scoped to the
 * authenticated owner.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/content-items")
class ContentItemController(
    private val contentItemService: ContentItemService,
    private val readerService: ReaderService,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int,
    ): ContentItemListResponse = contentItemService.list(principal.id, page, pageSize)

    @GetMapping("/{id}/reader")
    fun reader(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ): ReaderResponse = readerService.reader(principal.id, id)
}
