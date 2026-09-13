package com.bagusna.catchuplater.common.error

import java.time.Instant

/**
 * Single, stable error representation returned by every API error path,
 * including Spring Security filter failures.
 */
data class ApiError(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val fieldErrors: List<FieldValidationError> = emptyList(),
)

data class FieldValidationError(
    val field: String,
    val message: String,
)
