package com.bagusna.catchuplater.features.content.service

import com.bagusna.catchuplater.features.content.domain.ContentItem
import com.bagusna.catchuplater.features.content.dto.ContentItemListResponse
import com.bagusna.catchuplater.features.content.dto.ContentItemSummary
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side service for the Library. Processing status is exposed alongside
 * reading metadata so failed or in-flight captures can be surfaced inline
 * (DEC-001).
 */
@Service
class ContentItemService(
    private val contentItems: ContentItemRepository,
    private val artifacts: ArtifactVersionRepository,
) {
    @Transactional(readOnly = true)
    fun list(ownerId: Int, page: Int, pageSize: Int): ContentItemListResponse {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val result = contentItems.findAllByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            ownerId,
            PageRequest.of(safePage, safeSize),
        )
        val itemIds = result.content.mapNotNull { it.id }
        val readingTimes = if (itemIds.isEmpty()) {
            emptyMap()
        } else {
            artifacts.findByContentItemIdInAndIsCurrentTrue(itemIds)
                .associate { it.contentItemId to it.readingTimeMinutes }
        }

        return ContentItemListResponse(
            items = result.content.map { summarise(it, readingTimes[it.id]) },
            page = safePage,
            pageSize = safeSize,
            total = result.totalElements,
        )
    }

    fun summarise(item: ContentItem, readingTimeMinutes: Int?): ContentItemSummary =
        ContentItemSummary(
            id = requireNotNull(item.id),
            title = item.title,
            description = item.description,
            sourceUrl = item.sourceUrl,
            sourceName = item.sourceName,
            contentType = item.contentType.name,
            status = item.status.name,
            isFavorite = item.isFavorite,
            readingTimeMinutes = readingTimeMinutes,
            createdAt = item.createdAt,
            lastReadAt = item.lastReadAt,
        )

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
