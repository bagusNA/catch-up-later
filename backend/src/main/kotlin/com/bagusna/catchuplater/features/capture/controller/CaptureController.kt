package com.bagusna.catchuplater.features.capture.controller

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.core.security.AppUserPrincipal
import com.bagusna.catchuplater.features.capture.dto.CaptureResponse
import com.bagusna.catchuplater.features.capture.service.CaptureService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Capture intake and status. The browser extension authenticates with a bearer
 * token; the web client is not expected to call these endpoints.
 */
@RestController
@RequestMapping("${ApiRoutes.V1}/captures")
class CaptureController(
    private val captureService: CaptureService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @RequestHeader(name = IDEMPOTENCY_HEADER, required = false) idempotencyKey: String?,
        @RequestPart("package") packageFile: MultipartFile,
    ): CaptureResponse =
        captureService.intake(principal.id, idempotencyKey.orEmpty(), packageFile)

    @GetMapping("/{captureId}")
    fun status(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable captureId: String,
    ): CaptureResponse = captureService.status(principal.id, captureId)

    companion object {
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
    }
}
