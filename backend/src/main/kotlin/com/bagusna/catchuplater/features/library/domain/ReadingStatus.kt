package com.bagusna.catchuplater.features.library.domain

/**
 * Reading progress status, deliberately separate from the capture/processing
 * status on the content item (DEC-006).
 */
enum class ReadingStatus {
    UNREAD,
    IN_PROGRESS,
    READ,
}
