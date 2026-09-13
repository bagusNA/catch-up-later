package com.bagusna.catchuplater.features.library.service

import com.bagusna.catchuplater.common.error.ResourceNotFoundException
import com.bagusna.catchuplater.features.capture.dto.CaptureWarningDto
import com.bagusna.catchuplater.features.capture.packaging.CaptureManifest
import com.bagusna.catchuplater.features.content.domain.ArtifactVersion
import com.bagusna.catchuplater.features.content.dto.ContentItemDetailResponse
import com.bagusna.catchuplater.features.content.repository.ArtifactAssetRepository
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.content.service.ContentItemService
import com.bagusna.catchuplater.features.content.service.toArtifactSummary
import com.bagusna.catchuplater.features.library.repository.ContentItemTagRepository
import com.bagusna.catchuplater.features.library.repository.ReadingStateRepository
import com.bagusna.catchuplater.features.storage.ArtifactStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Mutations and the detail view for a library item: favorite toggle, soft
 * delete with artifact/index cleanup, and metadata inspection.
 */
@Service
class LibraryService(
    private val contentItems: ContentItemRepository,
    private val artifacts: ArtifactVersionRepository,
    private val assets: ArtifactAssetRepository,
    private val readingStates: ReadingStateRepository,
    private val readingStateService: ReadingStateService,
    private val contentItemTags: ContentItemTagRepository,
    private val contentItemService: ContentItemService,
    private val searchIndex: SearchIndexService,
    private val storage: ArtifactStorage,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun detail(ownerId: Int, contentItemId: Int): ContentItemDetailResponse {
        val item = requireOwnedItem(ownerId, contentItemId)
        val versions = artifacts.findByContentItemIdOrderByVersionNumberDesc(contentItemId)
        val summary = contentItemService.summarise(item, versions.firstOrNull { it.isCurrent }?.readingTimeMinutes)
        return ContentItemDetailResponse(
            contentItem = summary,
            readingState = readingStateService.get(ownerId, contentItemId),
            artifacts = versions.map { it.toArtifactSummary() },
            warnings = currentWarnings(versions),
        )
    }

    @Transactional
    fun setFavorite(ownerId: Int, contentItemId: Int, favorite: Boolean): Boolean {
        val item = requireOwnedItem(ownerId, contentItemId)
        item.isFavorite = favorite
        contentItems.save(item)
        return favorite
    }

    /**
     * Soft-deletes the item and physically removes every artifact version,
     * asset, reading state, tag assignment, and search index row.
     */
    @Transactional
    fun delete(ownerId: Int, contentItemId: Int) {
        val item = requireOwnedItem(ownerId, contentItemId)
        val versions = artifacts.findByContentItemIdOrderByVersionNumberDesc(contentItemId)
        versions.forEach { version ->
            assets.deleteAllByArtifactVersionId(requireNotNull(version.id))
            storage.delete(version.storageKey)
        }
        artifacts.deleteAllByContentItemId(contentItemId)
        contentItemTags.deleteAllByContentItemId(contentItemId)
        readingStates.deleteByContentItemId(contentItemId)
        searchIndex.remove(contentItemId)

        item.deletedAt = Instant.now()
        contentItems.saveAndFlush(item)
    }

    @Transactional(readOnly = true)
    fun requireOwnedItem(ownerId: Int, contentItemId: Int) =
        contentItems.findByIdAndOwnerIdAndDeletedAtIsNull(contentItemId, ownerId)
            ?: throw ResourceNotFoundException("The content item was not found.")

    private fun currentWarnings(versions: List<ArtifactVersion>): List<CaptureWarningDto> {
        val current = versions.firstOrNull { it.isCurrent } ?: return emptyList()
        return try {
            val manifest = storage.open(current.manifestStorageKey).use { stream ->
                objectMapper.readValue(stream, CaptureManifest::class.java)
            }
            manifest.capture.warnings.map { CaptureWarningDto(it.code, it.message) }
        } catch (exception: Exception) {
            emptyList()
        }
    }
}
