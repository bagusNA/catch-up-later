package com.bagusna.catchuplater.features.library.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.content.dto.TagResponse
import com.bagusna.catchuplater.features.library.service.TagService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Tag CRUD. Assignment lives on the content item (`PUT /content-items/{id}/tags`).
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/tags")
class TagController(
    private val tagService: TagService,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: AppUserPrincipal): List<TagResponse> =
        tagService.list(principal.id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: CreateTagRequest,
    ): TagResponse = tagService.create(principal.id, request.name)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Int,
    ) = tagService.delete(principal.id, id)
}

data class CreateTagRequest(
    @field:NotBlank(message = "A tag name is required.")
    @field:Size(max = TagService.MAX_NAME_LENGTH, message = "A tag name is too long.")
    val name: String = "",
)
