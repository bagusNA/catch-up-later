package com.bagusna.catchuplater.features.content.domain

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
 * The stable, mutable library entry. Renaming, favoriting, or deleting a
 * content item never changes the immutable artifact it points at.
 */
@Entity
@Table(name = "content_items")
class ContentItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "owner_id", nullable = false)
    var ownerId: Int = 0,

    @Column(nullable = false, length = 500)
    var title: String = "",

    @Column(length = 2000)
    var description: String? = null,

    @Column(name = "source_url", nullable = false, length = 2048)
    var sourceUrl: String = "",

    @Column(name = "canonical_url", length = 2048)
    var canonicalUrl: String? = null,

    @Column(name = "source_name", length = 255)
    var sourceName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 16)
    var contentType: ContentType = ContentType.ARTICLE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: ContentStatus = ContentStatus.PROCESSING,

    @Column(name = "is_favorite", nullable = false)
    var isFavorite: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH,

    @Column(name = "last_read_at")
    var lastReadAt: Instant? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,
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
