package com.bagusna.catchuplater.features.content.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant

/**
 * One immutable capture of a content item. A refresh creates a new version;
 * existing versions are never mutated.
 */
@Entity
@Table(name = "artifact_versions")
class ArtifactVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "content_item_id", nullable = false)
    var contentItemId: Int = 0,

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 32)
    var artifactType: ArtifactType = ArtifactType.ARTICLE_READER,

    /** Storage-relative directory containing the artifact files. */
    @Column(name = "storage_key", nullable = false, length = 512)
    var storageKey: String = "",

    @Column(name = "manifest_storage_key", nullable = false, length = 512)
    var manifestStorageKey: String = "",

    @Column(nullable = false, length = 64)
    var checksum: String = "",

    @Column(name = "byte_size", nullable = false)
    var byteSize: Long = 0,

    @Column(name = "reading_time_minutes")
    var readingTimeMinutes: Int? = null,

    @Column(name = "captured_at", nullable = false)
    var capturedAt: Instant = Instant.EPOCH,

    @Column(name = "adapter_id", nullable = false, length = 100)
    var adapterId: String = "",

    @Column(name = "adapter_version", nullable = false, length = 50)
    var adapterVersion: String = "",

    @Column(name = "package_schema_version", nullable = false)
    var packageSchemaVersion: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 32)
    var validationStatus: ValidationStatus = ValidationStatus.PENDING,

    @Column(name = "is_current", nullable = false)
    var isCurrent: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,
) {
    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }
}
