package com.bagusna.catchuplater.common.error

/**
 * Canonical error envelope returned by every API error path, including Spring
 * Security filter failures.
 *
 * Shape (see `specifications/11-implementation/02-decisions.md`, DEC-002):
 * ```json
 * { "error": { "code": "...", "message": "...", "details": {}, "requestId": "req_..." } }
 * ```
 */
data class ApiError(
    val error: ApiErrorBody,
)

data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
    val requestId: String,
)

data class FieldValidationError(
    val field: String,
    val message: String,
)
