package com.bagusna.catchuplater.features.library.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant

/**
 * Assignment of one [Tag] to one content item. Modeled explicitly (rather than
 * as a JPA `@ManyToMany`) so assignment and removal are plain, auditable rows.
 */
@Entity
@Table(name = "content_item_tags")
class ContentItemTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "content_item_id", nullable = false)
    var contentItemId: Int = 0,

    @Column(name = "tag_id", nullable = false)
    var tagId: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }
}
