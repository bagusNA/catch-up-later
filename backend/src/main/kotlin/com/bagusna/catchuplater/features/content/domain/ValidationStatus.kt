package com.bagusna.catchuplater.features.content.domain

/**
 * Outcome of backend package validation for an artifact version.
 */
enum class ValidationStatus {
    PENDING,
    VALID,
    VALID_WITH_WARNINGS,
    INVALID,
}
