package com.bagusna.catchuplater.features.content.domain

/**
 * Capture/processing status of a content item. This is deliberately distinct
 * from the reading status (see DEC-006).
 */
enum class ContentStatus {
    QUEUED,
    UPLOADING,
    PROCESSING,
    READY,
    FAILED,
    CANCELLED,
}
