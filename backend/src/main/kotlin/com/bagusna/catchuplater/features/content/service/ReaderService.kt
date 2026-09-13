package com.bagusna.catchuplater.features.content.service

import com.bagusna.catchuplater.common.api.ApiRoutes
import com.bagusna.catchuplater.common.error.ArtifactUnavailableException
import com.bagusna.catchuplater.common.error.ResourceNotFoundException
import com.bagusna.catchuplater.features.capture.dto.CaptureWarningDto
import com.bagusna.catchuplater.features.capture.packaging.CaptureManifest
import com.bagusna.catchuplater.features.capture.packaging.CapturePackagePaths
import com.bagusna.catchuplater.features.content.domain.ArtifactAsset
import com.bagusna.catchuplater.features.content.domain.ArtifactVersion
import com.bagusna.catchuplater.features.content.domain.ValidationStatus
import com.bagusna.catchuplater.features.content.dto.ArtifactManifestResponse
import com.bagusna.catchuplater.features.content.dto.ManifestAssetResponse
import com.bagusna.catchuplater.features.content.dto.ReaderMetadata
import com.bagusna.catchuplater.features.content.dto.ReaderResponse
import com.bagusna.catchuplater.features.content.repository.ArtifactAssetRepository
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.storage.ArtifactStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.io.InputStream

data class StoredAssetStream(
    val asset: ArtifactAsset,
    val stream: InputStream,
)

/**
 * Ownership-checked access to immutable artifacts. The reader HTML is returned
 * with package-local asset placeholders rewritten to artifact-scoped API URLs,
 * so the reader never depends on the original site.
 */
@Service
class ReaderService(
    private val contentItems: ContentItemRepository,
    private val artifacts: ArtifactVersionRepository,
    private val assets: ArtifactAssetRepository,
    private val storage: ArtifactStorage,
    private val contentItemService: ContentItemService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun reader(ownerId: Int, contentItemId: Int): ReaderResponse {
        val item = contentItems.findByIdAndOwnerIdAndDeletedAtIsNull(contentItemId, ownerId)
            ?: throw ArtifactUnavailableException("The content item was not found.")
        val artifact = artifacts.findByContentItemIdAndIsCurrentTrue(contentItemId)
            ?: throw ArtifactUnavailableException()
        if (artifact.validationStatus == ValidationStatus.INVALID) {
            throw ArtifactUnavailableException("The stored artifact did not pass validation.")
        }

        val html = readStoredText("${artifact.storageKey}/${CapturePackagePaths.HTML}")
        val manifest = readManifest(artifact)
        val assetBase = "${ApiRoutes.V1}/artifacts/${artifact.id}/assets/"
        val rewritten = html.replace(CapturePackagePaths.RESERVED_ASSET_PREFIX, assetBase)

        return ReaderResponse(
            contentItem = contentItemService.summarise(item, artifact.readingTimeMinutes),
            artifact = artifact.toArtifactSummary(),
            metadata = ReaderMetadata(
                author = manifest.metadata.author,
                description = manifest.metadata.description,
                siteName = manifest.metadata.siteName,
                publishedAt = manifest.metadata.publishedAt,
                language = manifest.artifact.language,
                readingTimeMinutes = artifact.readingTimeMinutes,
            ),
            html = rewritten,
            warnings = manifest.capture.warnings.map { CaptureWarningDto(it.code, it.message) },
        )
    }

    @Transactional(readOnly = true)
    fun manifest(ownerId: Int, artifactId: Int): ArtifactManifestResponse {
        val artifact = requireOwnedArtifact(ownerId, artifactId)
        val manifestBytes = readStoredBytes(artifact.manifestStorageKey)
        @Suppress("UNCHECKED_CAST")
        val manifestMap = objectMapper.readValue(manifestBytes, Map::class.java) as Map<String, Any?>
        val assetEntities = assets.findByArtifactVersionIdOrderByAssetKey(artifactId)
        val assetBase = "${ApiRoutes.V1}/artifacts/$artifactId/assets"

        return ArtifactManifestResponse(
            artifact = artifact.toArtifactSummary(),
            manifest = manifestMap,
            assets = assetEntities.map { asset ->
                ManifestAssetResponse(
                    assetKey = asset.assetKey,
                    mimeType = asset.mimeType,
                    byteSize = asset.byteSize,
                    checksum = asset.checksum,
                    originalUrl = asset.originalUrl,
                    altText = asset.altText,
                    width = asset.width,
                    height = asset.height,
                    url = "$assetBase/${asset.assetKey}",
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun openAsset(ownerId: Int, artifactId: Int, assetKey: String): StoredAssetStream {
        requireOwnedArtifact(ownerId, artifactId)
        val asset = assets.findByArtifactVersionIdAndAssetKey(artifactId, assetKey)
            ?: throw ResourceNotFoundException("The asset was not found.")
        return StoredAssetStream(asset, storage.open(asset.storageKey))
    }

    private fun requireOwnedArtifact(ownerId: Int, artifactId: Int): ArtifactVersion {
        val artifact = artifacts.findById(artifactId).orElse(null)
            ?: throw ArtifactUnavailableException()
        contentItems.findByIdAndOwnerIdAndDeletedAtIsNull(artifact.contentItemId, ownerId)
            ?: throw ArtifactUnavailableException()
        return artifact
    }

    private fun readManifest(artifact: ArtifactVersion): CaptureManifest =
        objectMapper.readValue(readStoredBytes(artifact.manifestStorageKey), CaptureManifest::class.java)

    private fun readStoredBytes(storageKey: String): ByteArray =
        storage.open(storageKey).use { it.readBytes() }

    private fun readStoredText(storageKey: String): String =
        String(readStoredBytes(storageKey), Charsets.UTF_8)
}
