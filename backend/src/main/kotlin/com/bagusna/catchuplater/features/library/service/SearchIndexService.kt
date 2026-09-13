package com.bagusna.catchuplater.features.library.service

import com.bagusna.catchuplater.features.capture.packaging.CaptureManifest
import com.bagusna.catchuplater.features.capture.packaging.CapturePackagePaths
import com.bagusna.catchuplater.features.content.domain.ArtifactVersion
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.library.repository.ContentItemTagRepository
import com.bagusna.catchuplater.features.library.repository.TagRepository
import com.bagusna.catchuplater.features.storage.ArtifactStorage
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/**
 * Owns the SQLite FTS5 `content_search` index. Application code (not database
 * triggers) writes rows so capture-time indexing and admin reindexing share the
 * same path and integrity checks are meaningful.
 */
@Service
class SearchIndexService(
    private val jdbc: JdbcTemplate,
    private val contentItems: ContentItemRepository,
    private val artifacts: ArtifactVersionRepository,
    private val contentItemTags: ContentItemTagRepository,
    private val tags: TagRepository,
    private val storage: ArtifactStorage,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class SearchHit(val contentItemId: Int, val rank: Double)

    data class IndexIntegrity(
        val indexedCount: Long,
        val missingCount: Long,
        val orphanCount: Long,
        val missingContentItemIds: List<Int>,
        val orphanContentItemIds: List<Int>,
    )

    /** Inserts or refreshes the index row for one item. Never throws. */
    fun index(contentItemId: Int) {
        try {
            val item = contentItems.findById(contentItemId).orElse(null) ?: return remove(contentItemId)
            if (item.deletedAt != null) return remove(contentItemId)
            val artifact = artifacts.findByContentItemIdAndIsCurrentTrue(contentItemId)

            val manifest = artifact?.let { readManifest(it) }
            val tagNames = tagNames(contentItemId, item.ownerId)
            val bodyText = artifact?.let { readBodyText(it) } ?: ""

            jdbc.update("DELETE FROM content_search WHERE content_item_id = ?", contentItemId)
            jdbc.update(
                """
                INSERT INTO content_search
                    (owner_id, content_item_id, artifact_id, title, description, author, source_name, body_text, tags)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                item.ownerId,
                contentItemId,
                artifact?.id,
                item.title,
                item.description,
                manifest?.metadata?.author,
                item.sourceName,
                bodyText,
                tagNames.joinToString(" "),
            )
        } catch (exception: Exception) {
            // Indexing failure is non-fatal: the artifact stays readable.
            log.warn("Failed to index content item {}: {}", contentItemId, exception.message)
        }
    }

    fun remove(contentItemId: Int) {
        jdbc.update("DELETE FROM content_search WHERE content_item_id = ?", contentItemId)
    }

    /**
     * Searches the index for one owner and returns matching ids ordered by
     * relevance (best first). User input is quoted into an FTS phrase so FTS
     * operators and malformed expressions cannot reach the query parser.
     */
    fun search(ownerId: Int, query: String): List<SearchHit> {
        val match = SearchQuery.toMatchExpression(query) ?: return emptyList()
        return jdbc.query(
            """
            SELECT content_item_id, bm25(content_search) AS rank
            FROM content_search
            WHERE content_search MATCH ? AND owner_id = ?
            ORDER BY rank
            LIMIT 1000
            """.trimIndent(),
            { rs, _ -> SearchHit(rs.getInt("content_item_id"), rs.getDouble("rank")) },
            match,
            ownerId,
        )
    }

    /** Rebuilds the index for one item from stored artifacts and metadata. */
    fun reindexItem(ownerId: Int, contentItemId: Int): Boolean {
        val item = contentItems.findByIdAndOwnerIdAndDeletedAtIsNull(contentItemId, ownerId) ?: return false
        index(requireNotNull(item.id))
        return true
    }

    /** Rebuilds the whole index. Returns the number of indexed items. */
    fun reindexAll(ownerId: Int): Int {
        jdbc.update("DELETE FROM content_search WHERE owner_id = ?", ownerId)
        val items = contentItems.findAllByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            ownerId,
            org.springframework.data.domain.Pageable.unpaged(),
        ).content
        items.forEach { item -> index(requireNotNull(item.id)) }
        return items.size
    }

    fun integrity(ownerId: Int): IndexIntegrity {
        val indexedIds = jdbc.queryForList(
            "SELECT content_item_id FROM content_search WHERE owner_id = ?",
            Int::class.java,
            ownerId,
        ).filterNotNull().toSet()
        val activeIds = contentItems
            .findAllByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                ownerId,
                org.springframework.data.domain.Pageable.unpaged(),
            )
            .content
            .mapNotNull { it.id }
            .toSet()

        val missing = (activeIds - indexedIds).sorted()
        val orphan = (indexedIds - activeIds).sorted()
        return IndexIntegrity(
            indexedCount = indexedIds.size.toLong(),
            missingCount = missing.size.toLong(),
            orphanCount = orphan.size.toLong(),
            missingContentItemIds = missing,
            orphanContentItemIds = orphan,
        )
    }

    private fun tagNames(contentItemId: Int, ownerId: Int): List<String> {
        val assignments = contentItemTags.findAllByContentItemId(contentItemId)
        if (assignments.isEmpty()) return emptyList()
        val byId = tags.findAllById(assignments.map { it.tagId }).associateBy { it.id }
        return assignments
            .mapNotNull { byId[it.tagId] }
            .filter { it.ownerId == ownerId }
            .map { it.name }
    }

    private fun readManifest(artifact: ArtifactVersion): CaptureManifest? = try {
        objectMapper.readValue(readBytes(artifact.manifestStorageKey), CaptureManifest::class.java)
    } catch (exception: Exception) {
        log.debug("Could not read manifest for artifact {}: {}", artifact.id, exception.message)
        null
    }

    private fun readBodyText(artifact: ArtifactVersion): String {
        val key = "${artifact.storageKey}/${CapturePackagePaths.TEXT}"
        if (!storage.exists(key)) return ""
        return try {
            String(readBytes(key), Charsets.UTF_8)
        } catch (exception: Exception) {
            log.debug("Could not read body text for artifact {}: {}", artifact.id, exception.message)
            ""
        }
    }

    private fun readBytes(storageKey: String): ByteArray = storage.open(storageKey).use { it.readBytes() }
}
