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
 * A user-owned label. Names are unique per owner case-insensitively through
 * [normalizedName], so "Reading" and "reading" are the same tag.
 */
@Entity
@Table(name = "tags")
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "owner_id", nullable = false)
    var ownerId: Int = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(name = "normalized_name", nullable = false, length = 100)
    var normalizedName: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }
}
