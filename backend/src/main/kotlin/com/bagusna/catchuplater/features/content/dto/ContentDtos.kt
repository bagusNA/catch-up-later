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
    val readingStatus: String,
    val isFavorite: Boolean,
    val readingTimeMinutes: Int?,
    val tags: List<String>,
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
    val versionNumber: Int,
    val isCurrent: Boolean,
    val artifactType: String,
    val checksum: String,
    val byteSize: Long,
    val readingTimeMinutes: Int?,
    val capturedAt: Instant,
    val adapterId: String,
    val adapterVersion: String,
    val packageSchemaVersion: Int,
    val validationStatus: String,
)

data class TagRef(
    val id: Int,
    val name: String,
)

data class PositionDto(
    val type: String?,
    val value: Long?,
)

data class ReadingStateResponse(
    val contentItemId: Int,
    val status: String,
    val progressPercent: Double?,
    val position: PositionDto?,
    val lastReadAt: Instant?,
    val updatedAt: Instant?,
)

data class ContentItemDetailResponse(
    val contentItem: ContentItemSummary,
    val readingState: ReadingStateResponse,
    val artifacts: List<ArtifactSummary>,
    val warnings: List<CaptureWarningDto>,
)

data class ReaderMetadata(
    val author: String?,
    val description: String?,
    val siteName: String?,
    val publishedAt: String?,
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

data class TagResponse(
    val id: Int,
    val name: String,
    val itemCount: Long,
    val createdAt: Instant,
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
