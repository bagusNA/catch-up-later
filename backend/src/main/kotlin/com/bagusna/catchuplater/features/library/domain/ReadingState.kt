package com.bagusna.catchuplater.features.library.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

/**
 * Per-content-item reading progress. Created lazily the first time an item is
 * opened or its status changes; absent rows mean [`ReadingStatus.UNREAD`].
 */
@Entity
@Table(name = "reading_states")
class ReadingState(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "owner_id", nullable = false)
    var ownerId: Int = 0,

    @Column(name = "content_item_id", nullable = false)
    var contentItemId: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: ReadingStatus = ReadingStatus.UNREAD,

    @Column(name = "progress_percent")
    var progressPercent: Double? = null,

    @Column(name = "position_type", length = 32)
    var positionType: String? = null,

    @Column(name = "position_value")
    var positionValue: Long? = null,

    @Column(name = "last_read_at")
    var lastReadAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
) {
    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
