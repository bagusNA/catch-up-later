package com.bagusna.catchuplater.features.library.service

import com.bagusna.catchuplater.features.content.domain.ContentItem
import com.bagusna.catchuplater.features.content.domain.ContentStatus
import com.bagusna.catchuplater.features.content.domain.ContentType
import com.bagusna.catchuplater.features.content.dto.ContentItemListResponse
import com.bagusna.catchuplater.features.content.dto.ContentItemSummary
import com.bagusna.catchuplater.features.content.service.ContentItemService
import com.bagusna.catchuplater.features.library.domain.ReadingStatus
import com.bagusna.catchuplater.features.library.repository.ContentItemTagRepository
import com.bagusna.catchuplater.features.library.repository.TagRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

/**
 * Owner-scoped library listing: filters, sort, and pagination over the main
 * database.
 *
 * Full-text matches come from the FTS5 sidecar database, so search is done in
 * two steps: the index returns matching item ids ordered by relevance, then the
 * main query applies the remaining filters. Result count always matches the
 * returned page.
 */
@Service
class LibraryQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val searchIndex: SearchIndexService,
    private val contentItemTags: ContentItemTagRepository,
    private val tags: TagRepository,
) {
    @Transactional(readOnly = true)
    fun list(
        ownerId: Int,
        query: String?,
        contentType: String?,
        readingStatus: String?,
        favorite: Boolean?,
        tag: String?,
        sort: String?,
        page: Int,
        pageSize: Int,
    ): ContentItemListResponse {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val searchHits = trimmedQuery?.let { searchIndex.search(ownerId, it) }
        if (searchHits != null && searchHits.isEmpty()) {
            return ContentItemListResponse(emptyList(), safePage, safeSize, 0)
        }

        val parameters = MapSqlParameterSource()
            .addValue("ownerId", ownerId)
            .addValue("limit", safeSize)
            .addValue("offset", safePage.toLong() * safeSize)

        val from = StringBuilder(
            """
            FROM content_items ci
            LEFT JOIN reading_states rs ON rs.content_item_id = ci.id AND rs.owner_id = ci.owner_id
            LEFT JOIN artifact_versions av ON av.content_item_id = ci.id AND av.is_current = 1
            """.trimIndent(),
        )
        val where = StringBuilder(" WHERE ci.owner_id = :ownerId AND ci.deleted_at IS NULL")

        searchHits?.let { hits ->
            where.append(" AND ci.id IN (:searchIds)")
            parameters.addValue("searchIds", hits.map { it.contentItemId })
        }
        contentType?.let {
            where.append(" AND ci.content_type = :contentType")
            parameters.addValue("contentType", it.uppercase())
        }
        favorite?.let {
            where.append(" AND ci.is_favorite = :favorite")
            parameters.addValue("favorite", it)
        }
        readingStatus?.let {
            where.append(" AND COALESCE(rs.status, 'UNREAD') = :readingStatus")
            parameters.addValue("readingStatus", it.uppercase())
        }
        tag?.trim()?.takeIf { it.isNotEmpty() }?.let {
            where.append(
                " " + """
                AND EXISTS (
                   SELECT 1 FROM content_item_tags cit
                   JOIN tags t ON t.id = cit.tag_id
                   WHERE cit.content_item_id = ci.id
                     AND t.owner_id = :ownerId
                     AND t.normalized_name = :tag
                )
                """.trimIndent(),
            )
            parameters.addValue("tag", TagService.normalize(it))
        }

        val effectiveSort = effectiveSort(sort, searchHits != null)
        val total = jdbc.queryForObject("SELECT COUNT(*)$from$where", parameters, Long::class.java) ?: 0L

        val select = """
            SELECT ci.id,
                   ci.title,
                   ci.description,
                   ci.source_url,
                   ci.source_name,
                   ci.content_type,
                   ci.status,
                   ci.is_favorite,
                   ci.created_at,
                   ci.last_read_at,
                   av.reading_time_minutes,
                   COALESCE(rs.status, 'UNREAD') AS reading_status
            $from$where
        """.trimIndent()

        val rows = if (effectiveSort == RELEVANCE) {
            // Relevance ordering lives in the sidecar index, so order the
            // filtered rows in memory before paginating.
            val order = searchHits.orEmpty().mapIndexed { index, hit -> hit.contentItemId to index }.toMap()
            val all = jdbc.query(select, parameters) { rs, _ -> rs.toRow() }
            all.sortedBy { order[it.contentItem.id] ?: Int.MAX_VALUE }
                .drop(safePage * safeSize)
                .take(safeSize)
        } else {
            jdbc.query("$select\n${orderBy(effectiveSort)}\nLIMIT :limit OFFSET :offset", parameters) { rs, _ ->
                rs.toRow()
            }
        }

        val tagNames = tagNamesByItem(rows.mapNotNull { it.contentItem.id }, ownerId)
        val items = rows.map { row -> row.toSummary(tagNames[row.contentItem.id].orEmpty()) }
        return ContentItemListResponse(items = items, page = safePage, pageSize = safeSize, total = total)
    }

    private fun effectiveSort(sort: String?, hasQuery: Boolean): String {
        val requested = when (sort?.lowercase()) {
            "title" -> "title"
            "last_read", "lastread" -> "last_read"
            "saved" -> "saved"
            "relevance" -> if (hasQuery) RELEVANCE else "saved"
            null, "" -> if (hasQuery) RELEVANCE else "saved"
            else -> "saved"
        }
        return requested
    }

    private fun orderBy(sort: String): String = when (sort) {
        "title" -> "ORDER BY ci.title COLLATE NOCASE ASC, ci.id ASC"
        "last_read" -> "ORDER BY (rs.last_read_at IS NULL) ASC, rs.last_read_at DESC, ci.id DESC"
        else -> "ORDER BY ci.created_at DESC, ci.id DESC"
    }

    private fun tagNamesByItem(itemIds: List<Int>, ownerId: Int): Map<Int, List<String>> {
        if (itemIds.isEmpty()) return emptyMap()
        val assignments = contentItemTags.findAllByContentItemIdIn(itemIds)
        if (assignments.isEmpty()) return emptyMap()
        val tagsById = tags.findAllById(assignments.map { it.tagId }).filter { it.ownerId == ownerId }.associateBy { it.id }
        return assignments
            .groupBy({ it.contentItemId }, { tagsById[it.tagId]?.name })
            .mapValues { (_, names) -> names.filterNotNull().sorted() }
    }

    private data class Row(
        val contentItem: ContentItem,
        val readingTimeMinutes: Int?,
        val readingStatus: ReadingStatus,
    ) {
        fun toSummary(tagNames: List<String>): ContentItemSummary {
            val item = contentItem
            return ContentItemSummary(
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
        }
    }

    private fun ResultSet.toRow(): Row {
        val item = ContentItem(
            id = getInt("id"),
            ownerId = 0,
            title = getString("title"),
            description = getString("description"),
            sourceUrl = getString("source_url"),
            sourceName = getString("source_name"),
            contentType = ContentType.valueOf(getString("content_type")),
            status = ContentStatus.valueOf(getString("status")),
            isFavorite = getBoolean("is_favorite"),
            createdAt = getTimestamp("created_at").toInstant(),
            lastReadAt = getTimestamp("last_read_at")?.toInstant(),
        )
        return Row(
            contentItem = item,
            readingTimeMinutes = getObject("reading_time_minutes") as? Int,
            readingStatus = ReadingStatus.valueOf(getString("reading_status")),
        )
    }

    companion object {
        const val MAX_PAGE_SIZE = ContentItemService.MAX_PAGE_SIZE
        private const val RELEVANCE = "relevance"
    }
}
