package com.bagusna.catchuplater.features.capture.domain

/**
 * Lifecycle of a capture job. Mirrors [com.bagusna.catchuplater.features.content.domain.ContentStatus]
 * but is tracked independently so a failed upload can be polled and retried.
 */
enum class CaptureStatus {
    QUEUED,
    UPLOADING,
    PROCESSING,
    READY,
    FAILED,
    CANCELLED,
}
