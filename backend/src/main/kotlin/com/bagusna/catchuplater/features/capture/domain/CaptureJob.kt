package com.bagusna.catchuplater.features.capture.domain

import com.bagusna.catchuplater.features.content.domain.ArtifactType
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
 * One upload attempt. The `(ownerId, idempotencyKey)` pair is unique so a
 * retried upload returns the original result instead of creating a duplicate.
 */
@Entity
@Table(name = "capture_jobs")
class CaptureJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "owner_id", nullable = false)
    var ownerId: Int = 0,

    /** Public capture identifier returned to the client (`captureId`). */
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    var publicId: String = "",

    @Column(name = "idempotency_key", nullable = false, length = 128)
    var idempotencyKey: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: CaptureStatus = CaptureStatus.QUEUED,

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", length = 32)
    var artifactType: ArtifactType? = null,

    @Column(name = "source_url", length = 2048)
    var sourceUrl: String? = null,

    @Column(name = "content_item_id")
    var contentItemId: Int? = null,

    @Column(name = "artifact_version_id")
    var artifactVersionId: Int? = null,

    /** Serialized JSON array of capture warnings. */
    @Column(length = 4000)
    var warnings: String? = null,

    @Column(name = "error_code", length = 64)
    var errorCode: String? = null,

    @Column(name = "error_message", length = 1024)
    var errorMessage: String? = null,

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

    fun markReady(
        contentItemId: Int,
        artifactVersionId: Int,
        artifactType: ArtifactType,
        sourceUrl: String,
        warningsJson: String?,
    ) {
        status = CaptureStatus.READY
        this.contentItemId = contentItemId
        this.artifactVersionId = artifactVersionId
        this.artifactType = artifactType
        this.sourceUrl = sourceUrl
        this.warnings = warningsJson
        errorCode = null
        errorMessage = null
    }

    fun markFailed(code: String, message: String) {
        status = CaptureStatus.FAILED
        errorCode = code
        errorMessage = message.take(1024)
    }
}
