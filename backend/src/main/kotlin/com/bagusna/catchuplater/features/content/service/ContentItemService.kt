package com.bagusna.catchuplater.features.content.service

import com.bagusna.catchuplater.features.content.domain.ContentItem
import com.bagusna.catchuplater.features.content.dto.ContentItemSummary
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.library.domain.ReadingStatus
import com.bagusna.catchuplater.features.library.repository.ContentItemTagRepository
import com.bagusna.catchuplater.features.library.repository.ReadingStateRepository
import com.bagusna.catchuplater.features.library.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Assembles the library-facing view of a content item: capture metadata plus
 * the user-controlled reading status and tags.
 */
@Service
class ContentItemService(
    private val contentItems: ContentItemRepository,
    private val artifacts: ArtifactVersionRepository,
    private val readingStates: ReadingStateRepository,
    private val contentItemTags: ContentItemTagRepository,
    private val tags: TagRepository,
) {
    @Transactional(readOnly = true)
    fun summarise(item: ContentItem, readingTimeMinutes: Int?): ContentItemSummary {
        val id = requireNotNull(item.id)
        val readingStatus = readingStates.findByContentItemIdAndOwnerId(id, item.ownerId)?.status
            ?: ReadingStatus.UNREAD
        val tagNames = tagNames(id, item.ownerId)
        return summarise(item, readingTimeMinutes, readingStatus, tagNames)
    }

    fun summarise(
        item: ContentItem,
        readingTimeMinutes: Int?,
        readingStatus: ReadingStatus,
        tagNames: List<String>,
    ): ContentItemSummary =
        ContentItemSummary(
            id = requireNotNull(item.id),
            title = item.title,
            description = item.description,
            sourceUrl = item.sourceUrl,
            sourceName = item.sourceName,
            contentType = item.contentType.name,
            status = item.status.name,
            readingStatus = readingStatus.name,
            isFavorite = item.isFavorite,
            readingTimeMinutes = readingTimeMinutes,
            tags = tagNames,
            createdAt = item.createdAt,
            lastReadAt = item.lastReadAt,
        )

    @Transactional(readOnly = true)
    fun readingTimeFor(itemId: Int): Int? =
        artifacts.findByContentItemIdAndIsCurrentTrue(itemId)?.readingTimeMinutes

    private fun tagNames(contentItemId: Int, ownerId: Int): List<String> {
        val assignments = contentItemTags.findAllByContentItemId(contentItemId)
        if (assignments.isEmpty()) return emptyList()
        val byId = tags.findAllById(assignments.map { it.tagId }).associateBy { it.id }
        return assignments
            .mapNotNull { byId[it.tagId] }
            .filter { it.ownerId == ownerId }
            .map { it.name }
            .sorted()
    }

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
