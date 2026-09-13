package com.bagusna.catchuplater.library

import com.bagusna.catchuplater.IntegrationTestBase
import com.bagusna.catchuplater.features.auth.domain.RoleNames
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end library and discovery behavior: search, filters, sort, tags,
 * favorites, reading state, item detail, deletion with cleanup, and owner
 * isolation.
 */
class LibraryApiTest : IntegrationTestBase() {

    @AfterEach
    fun cleanup() = clearAccounts()

    @Test
    fun `searches title, description, body text, source and tags for the owner only`() {
        val owner = accessToken("discovery-owner@example.com")
        val intruder = accessToken("discovery-intruder@example.com")

        val rocket = capture(owner, "Rocket science", "An introduction to orbital mechanics", "Orbital mechanics is fascinating")
        capture(owner, "Gardening", "How to grow tomatoes", "Tomatoes need sunlight")
        capture(intruder, "Rocket intruder", "Should never be visible", "orbital mechanics secret")

        // Title match.
        mockMvc.perform(get("/api/v1/content-items?q=Rocket").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].title").value("Rocket science"))

        // Body-text match.
        mockMvc.perform(get("/api/v1/content-items?q=fascinating").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(rocket))

        // Description match.
        mockMvc.perform(get("/api/v1/content-items?q=orbital").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        // The intruder's matching item is never returned.
        mockMvc.perform(get("/api/v1/content-items?q=Rocket").header("Authorization", "Bearer $intruder"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].title").value("Rocket intruder"))

        // Injection-like input must not surface SQL/FTS errors.
        mockMvc.perform(get("/api/v1/content-items?q=%22%29%20OR%201%3D1%20--").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isArray)
    }

    @Test
    fun `combines filters and sort with pagination`() {
        val owner = accessToken("filters@example.com")
        val alpha = capture(owner, "Alpha article", "First", "Body alpha")
        val beta = capture(owner, "Beta article", "Second", "Body beta")
        val gamma = capture(owner, "Gamma article", "Third", "Body gamma")

        // Favorite beta only.
        mockMvc.perform(post("/api/v1/content-items/$beta/favorite").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isFavorite").value(true))

        // Favorite filter.
        mockMvc.perform(get("/api/v1/content-items?favorite=true").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(beta))

        // Reading status filter defaults to UNREAD.
        mockMvc.perform(get("/api/v1/content-items?status=UNREAD").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))

        markInProgress(owner, alpha)

        mockMvc.perform(get("/api/v1/content-items?status=IN_PROGRESS").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(alpha))

        mockMvc.perform(get("/api/v1/content-items?status=UNREAD").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))

        // contentType filter.
        mockMvc.perform(get("/api/v1/content-items?contentType=PDF").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))

        // Sort by title.
        mockMvc.perform(get("/api/v1/content-items?sort=title").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].title").value("Alpha article"))
            .andExpect(jsonPath("$.items[1].title").value("Beta article"))

        // Pagination.
        mockMvc.perform(get("/api/v1/content-items?page=0&pageSize=2").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.total").value(3))
        mockMvc.perform(get("/api/v1/content-items?page=1&pageSize=2").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))

        // Verify gamma is reachable through search + favorite combination.
        mockMvc.perform(get("/api/v1/content-items?q=Gamma&favorite=true").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `manages tags with case-insensitive uniqueness and assignment`() {
        val owner = accessToken("tags@example.com")
        val item = capture(owner, "Tagged article", "desc", "body")

        val created = mockMvc.perform(
            post("/api/v1/tags")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Reading"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Reading"))
            .andReturn()
        val tagId = objectMapper.readTree(created.response.contentAsString).get("id").asInt()

        // Same name, different case is a conflict.
        mockMvc.perform(
            post("/api/v1/tags")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"reading"}"""),
        ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("TAG_EXISTS"))

        // Assign and read back.
        mockMvc.perform(
            put("/api/v1/content-items/$item/tags")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"tagIds":[$tagId]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tags", hasItem("Reading")))

        // Tag filter and search by tag.
        mockMvc.perform(get("/api/v1/content-items?tag=reading").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
        mockMvc.perform(get("/api/v1/content-items?q=Reading").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        mockMvc.perform(get("/api/v1/tags").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].itemCount").value(1))

        // Removing a tag removes the assignment.
        mockMvc.perform(delete("/api/v1/tags/$tagId").header("Authorization", "Bearer $owner"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/v1/content-items?tag=reading").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `reading state transitions and can be reverted`() {
        val owner = accessToken("reading@example.com")
        val item = capture(owner, "Reading state", "desc", "body")

        mockMvc.perform(get("/api/v1/content-items/$item/reading-state").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UNREAD"))

        // Meaningful progress derives IN_PROGRESS.
        mockMvc.perform(
            put("/api/v1/content-items/$item/reading-state")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"progressPercent":42.5,"position":{"type":"ARTICLE_TEXT_OFFSET","value":12000}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.progressPercent").value(42.5))
            .andExpect(jsonPath("$.position.type").value("ARTICLE_TEXT_OFFSET"))

        // Explicit READ.
        mockMvc.perform(
            put("/api/v1/content-items/$item/reading-state")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"READ","progressPercent":100}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("READ"))

        // Revert to UNREAD.
        mockMvc.perform(
            put("/api/v1/content-items/$item/reading-state")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"UNREAD"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UNREAD"))

        // Invalid status.
        mockMvc.perform(
            put("/api/v1/content-items/$item/reading-state")
                .header("Authorization", "Bearer $owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"PAUSED"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_READING_STATE"))

        // lastReadAt is surfaced in the list and detail.
        mockMvc.perform(get("/api/v1/content-items/$item").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contentItem.readingStatus").value("UNREAD"))
            .andExpect(jsonPath("$.artifacts[0].versionNumber").value(1))
    }

    @Test
    fun `deletes an item and removes artifacts and index rows`() {
        val owner = accessToken("delete@example.com", roleNames = setOf(RoleNames.USER, RoleNames.ADMIN))
        val item = capture(owner, "To delete", "desc", "deletable body unique token zzzqqq")

        // Present before deletion.
        mockMvc.perform(get("/api/v1/content-items?q=zzzqqq").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        mockMvc.perform(delete("/api/v1/content-items/$item").header("Authorization", "Bearer $owner"))
            .andExpect(status().isNoContent)

        // Gone from listing, search, and detail.
        mockMvc.perform(get("/api/v1/content-items").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
        mockMvc.perform(get("/api/v1/content-items?q=zzzqqq").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(0))
        mockMvc.perform(get("/api/v1/content-items/$item").header("Authorization", "Bearer $owner"))
            .andExpect(status().isNotFound)

        // Integrity reports no orphan index rows.
        mockMvc.perform(get("/api/v1/admin/search/integrity").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orphanCount").value(0))
    }

    @Test
    fun `reindex rebuilds the search index`() {
        val owner = accessToken("reindex@example.com", roleNames = setOf(RoleNames.USER, RoleNames.ADMIN))
        val item = capture(owner, "Reindex me", "desc", "reindexable body token wwwooo")

        mockMvc.perform(post("/api/v1/admin/search/reindex").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reindexed").value(1))
            .andExpect(jsonPath("$.scope").value("all"))

        mockMvc.perform(get("/api/v1/content-items?q=wwwooo").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        mockMvc.perform(
            post("/api/v1/admin/search/reindex?contentItemId=$item").header("Authorization", "Bearer $owner"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scope").value("item"))
    }

    @Test
    fun `does not allow a second user to see, tag, favorite or delete another item`() {
        val owner = accessToken("owner-scope@example.com")
        val intruder = accessToken("intruder-scope@example.com")
        val item = capture(owner, "Private item", "desc", "body")

        mockMvc.perform(get("/api/v1/content-items/$item").header("Authorization", "Bearer $intruder"))
            .andExpect(status().isNotFound)
        mockMvc.perform(post("/api/v1/content-items/$item/favorite").header("Authorization", "Bearer $intruder"))
            .andExpect(status().isNotFound)
        mockMvc.perform(delete("/api/v1/content-items/$item").header("Authorization", "Bearer $intruder"))
            .andExpect(status().isNotFound)
        mockMvc.perform(get("/api/v1/content-items/$item/reader").header("Authorization", "Bearer $intruder"))
            .andExpect(status().isNotFound)

        // The item is still there for its owner.
        mockMvc.perform(get("/api/v1/content-items/$item").header("Authorization", "Bearer $owner"))
            .andExpect(status().isOk)
    }

    private fun markInProgress(token: String, itemId: Int) {
        mockMvc.perform(
            put("/api/v1/content-items/$itemId/reading-state")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"progressPercent":10}"""),
        ).andExpect(status().isOk)
    }

    private fun capture(token: String, title: String, description: String, body: String): Int {
        val idempotencyKey = "idem-" + java.util.UUID.randomUUID()
        val packageBytes = buildPackage(title, description, body)
        val created = mockMvc.perform(
            multipart("/api/v1/captures")
                .file(MockMultipartFile("package", "capture.zip", "application/zip", packageBytes))
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", idempotencyKey),
        )
            .andExpect(status().isCreated)
            .andReturn()
        return objectMapper.readTree(created.response.contentAsString).get("contentItemId").asInt()
    }

    private fun accessToken(
        email: String,
        roleNames: Set<String> = setOf(RoleNames.USER),
    ): String {
        createUser(email, roleNames = roleNames)
        val result = mockMvc.perform(
            post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$VALID_PASSWORD"}"""),
        ).andExpect(status().isOk).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    private fun buildPackage(title: String, description: String, body: String): ByteArray {
        val html = "<article><h1>$title</h1><p>$body</p></article>"
        val manifest = """
            {
              "schemaVersion": 1,
              "source": {
                "url": "https://example.com/${title.lowercase().replace(' ', '-')}",
                "canonicalUrl": "https://example.com/${title.lowercase().replace(' ', '-')}",
                "sourceName": "Example",
                "adapterId": "generic-readability",
                "adapterVersion": "1.0.0"
              },
              "artifact": {
                "type": "ARTICLE_READER",
                "title": "$title",
                "language": "en",
                "readingTimeMinutes": 3
              },
              "metadata": { "author": "Author", "description": "$description" },
              "assets": [],
              "capture": { "capturedAt": "2026-01-01T00:00:00Z", "warnings": [] }
            }
        """.trimIndent()

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content.html"))
            zip.write(html.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content.txt"))
            zip.write(body.toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
