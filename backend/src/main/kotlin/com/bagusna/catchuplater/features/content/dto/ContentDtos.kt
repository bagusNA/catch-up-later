package com.bagusna.catchuplater.features.content.dto

import com.bagusna.catchuplater.features.capture.dto.CaptureWarningDto
import java.time.Instant

data class ContentItemSummary(
    val id: Int,
    val title: String,
    val description: String?,
    val sourceUrl: String,
    val sourceName: String?,
    val contentType: String,
    val status: String,
    val isFavorite: Boolean,
    val readingTimeMinutes: Int?,
    val createdAt: Instant,
    val lastReadAt: Instant?,
)

data class ContentItemListResponse(
    val items: List<ContentItemSummary>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
)

data class ArtifactSummary(
    val id: Int,
    val artifactType: String,
    val checksum: String,
    val byteSize: Long,
    val capturedAt: Instant,
    val adapterId: String,
    val adapterVersion: String,
    val packageSchemaVersion: Int,
    val validationStatus: String,
)

data class ReaderMetadata(
    val author: String?,
    val description: String?,
    val siteName: String?,
    val publishedAt: Instant?,
    val language: String?,
    val readingTimeMinutes: Int?,
)

data class ReaderResponse(
    val contentItem: ContentItemSummary,
    val artifact: ArtifactSummary,
    val metadata: ReaderMetadata,
    val html: String,
    val warnings: List<CaptureWarningDto>,
)

data class ManifestAssetResponse(
    val assetKey: String,
    val mimeType: String,
    val byteSize: Long,
    val checksum: String,
    val originalUrl: String?,
    val altText: String?,
    val width: Int?,
    val height: Int?,
    val url: String,
)

data class ArtifactManifestResponse(
    val artifact: ArtifactSummary,
    val manifest: Map<String, Any?>,
    val assets: List<ManifestAssetResponse>,
)
