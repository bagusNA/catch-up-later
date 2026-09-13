package com.bagusna.catchuplater.features.capture.dto

/**
 * Response for `POST /api/v1/captures` and `GET /api/v1/captures/{captureId}`.
 */
data class CaptureResponse(
    val captureId: String,
    val status: String,
    val contentItemId: Int? = null,
    val artifactId: Int? = null,
    val artifactType: String? = null,
    val sourceUrl: String? = null,
    val warnings: List<CaptureWarningDto> = emptyList(),
    val error: CaptureErrorDto? = null,
)

data class CaptureWarningDto(
    val code: String,
    val message: String,
)

data class CaptureErrorDto(
    val code: String,
    val message: String,
)
