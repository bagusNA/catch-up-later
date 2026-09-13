package com.bagusna.catchuplater.features.library.service

import com.bagusna.catchuplater.common.error.DuplicateTagException
import com.bagusna.catchuplater.common.error.InvalidTagException
import com.bagusna.catchuplater.common.error.ResourceNotFoundException
import com.bagusna.catchuplater.features.content.dto.TagResponse
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.library.domain.ContentItemTag
import com.bagusna.catchuplater.features.library.domain.Tag
import com.bagusna.catchuplater.features.library.repository.ContentItemTagRepository
import com.bagusna.catchuplater.features.library.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

/**
 * Tag CRUD and content-item assignment. Names are normalized (trimmed and
 * lowercased) and unique per owner.
 */
@Service
class TagService(
    private val tags: TagRepository,
    private val contentItemTags: ContentItemTagRepository,
    private val contentItems: ContentItemRepository,
    private val searchIndex: SearchIndexService,
) {
    @Transactional(readOnly = true)
    fun list(ownerId: Int): List<TagResponse> {
        val tags = tags.findAllByOwnerIdOrderByNameAsc(ownerId)
        if (tags.isEmpty()) return emptyList()
        val counts = contentItemTags.findAll()
            .groupingBy { it.tagId }
            .eachCount()
        return tags.map { tag ->
            TagResponse(
                id = requireNotNull(tag.id),
                name = tag.name,
                itemCount = (counts[tag.id] ?: 0).toLong(),
                createdAt = tag.createdAt,
            )
        }
    }

    @Transactional
    fun create(ownerId: Int, name: String): TagResponse {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.length > MAX_NAME_LENGTH) {
            throw InvalidTagException("A tag name of 1-$MAX_NAME_LENGTH characters is required.")
        }
        val normalized = normalize(cleanName)
        tags.findByOwnerIdAndNormalizedName(ownerId, normalized)?.let {
            throw DuplicateTagException()
        }
        val saved = tags.saveAndFlush(
            Tag(ownerId = ownerId, name = cleanName, normalizedName = normalized),
        )
        return TagResponse(
            id = requireNotNull(saved.id),
            name = saved.name,
            itemCount = 0,
            createdAt = saved.createdAt,
        )
    }

    @Transactional
    fun delete(ownerId: Int, tagId: Int) {
        val tag = tags.findByIdAndOwnerId(tagId, ownerId) ?: throw ResourceNotFoundException("The tag was not found.")
        val affectedItems = contentItemTags.findAllByTagId(requireNotNull(tag.id)).map { it.contentItemId }.distinct()
        contentItemTags.deleteAllByTagId(requireNotNull(tag.id))
        tags.delete(tag)
        affectedItems.forEach { searchIndex.index(it) }
    }

    @Transactional
    fun assign(ownerId: Int, contentItemId: Int, tagIds: List<Int>) {
        val item = contentItems.findByIdAndOwnerIdAndDeletedAtIsNull(contentItemId, ownerId)
            ?: throw ResourceNotFoundException("The content item was not found.")
        val owned = tags.findAllById(tagIds.distinct()).filter { it.ownerId == ownerId }
        if (owned.size != tagIds.distinct().size) {
            throw ResourceNotFoundException("One or more tags were not found.")
        }
        contentItemTags.deleteAllByContentItemId(requireNotNull(item.id))
        contentItemTags.flush()
        owned.forEach { tag ->
            contentItemTags.save(
                ContentItemTag(contentItemId = requireNotNull(item.id), tagId = requireNotNull(tag.id)),
            )
        }
        contentItemTags.flush()
        searchIndex.index(requireNotNull(item.id))
    }

    @Transactional(readOnly = true)
    fun tagNamesFor(contentItemId: Int, ownerId: Int): List<String> {
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
        const val MAX_NAME_LENGTH = 100

        fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)
    }
}
