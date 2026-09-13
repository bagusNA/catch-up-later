package com.bagusna.catchuplater.features.content.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.content.dto.ArtifactManifestResponse
import com.bagusna.catchuplater.features.content.service.ReaderService
import jakarta.validation.constraints.Pattern
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/**
 * Ownership-checked artifact access. Artifact bytes are immutable, so asset
 * responses are aggressively cacheable.
 */
@Validated
@RestController
@RequestMapping("${ApiRoutes.V1}/artifacts")
class ArtifactController(
    private val readerService: ReaderService,
) {
    @GetMapping("/{artifactId}/manifest")
    fun manifest(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable artifactId: Int,
    ): ArtifactManifestResponse = readerService.manifest(principal.id, artifactId)

    @GetMapping("/{artifactId}/assets/{assetKey}")
    fun asset(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable artifactId: Int,
        @PathVariable
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
        assetKey: String,
    ): ResponseEntity<InputStreamResource> {
        val stored = readerService.openAsset(principal.id, artifactId, assetKey)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(stored.asset.mimeType))
            .contentLength(stored.asset.byteSize)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "default-src 'none'; sandbox")
            .body(InputStreamResource(stored.stream))
    }
}
