package com.bagusna.catchuplater.features.content.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A binary asset belonging to exactly one artifact version. Assets are only
 * ever addressed by [assetKey] within the artifact's storage directory, never
 * by a caller-supplied filesystem path.
 */
@Entity
@Table(name = "artifact_assets")
class ArtifactAsset(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "artifact_version_id", nullable = false)
    var artifactVersionId: Int = 0,

    @Column(name = "asset_key", nullable = false, length = 128)
    var assetKey: String = "",

    @Column(name = "mime_type", nullable = false, length = 100)
    var mimeType: String = "",

    @Column(name = "byte_size", nullable = false)
    var byteSize: Long = 0,

    @Column(nullable = false, length = 64)
    var checksum: String = "",

    @Column(name = "storage_key", nullable = false, length = 512)
    var storageKey: String = "",

    @Column(name = "original_url", length = 2048)
    var originalUrl: String? = null,

    @Column(name = "alt_text", length = 1000)
    var altText: String? = null,

    @Column
    var width: Int? = null,

    @Column
    var height: Int? = null,
)
