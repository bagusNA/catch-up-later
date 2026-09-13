package com.bagusna.catchuplater.capture

import com.bagusna.catchuplater.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end capture intake: upload a real ZIP package over multipart, verify
 * validation, sanitization, ownership-checked reading, and asset serving.
 */
class CaptureApiTest : IntegrationTestBase() {

    @AfterEach
    fun cleanup() = clearAccounts()

    @Test
    fun `saves a generic article and reads it back`() {
        val token = accessToken("capture@example.com")
        val png = onePixelPng()
        val packageBytes = buildPackage(
            html = """
                <article>
                  <h1>Example article</h1>
                  <p>Hello <script>alert('xss')</script>world.</p>
                  <img src="https://assets.cul.invalid/asset-1" alt="A dot">
                  <a href="https://example.com/more" onclick="steal()">More</a>
                </article>
            """.trimIndent(),
            text = "Example article. Hello world.",
            assets = listOf(Asset("asset-1", "image/png", png)),
        )

        val created = mockMvc.perform(
            multipart("/api/v1/captures")
                .file(MockMultipartFile("package", "capture.zip", "application/zip", packageBytes))
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", "idem-article-0001"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.contentItemId").isNumber)
            .andExpect(jsonPath("$.artifactId").isNumber)
            .andExpect(jsonPath("$.warnings").isArray)
            .andReturn()

        val body = objectMapper.readTree(created.response.contentAsString)
        val captureId = body.get("captureId").asText()
        val contentItemId = body.get("contentItemId").asInt()
        val artifactId = body.get("artifactId").asInt()

        // Status polling returns the same, persisted result.
        mockMvc.perform(get("/api/v1/captures/$captureId").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.contentItemId").value(contentItemId))

        // Idempotent retry with the same key returns the same capture.
        mockMvc.perform(
            multipart("/api/v1/captures")
                .file(MockMultipartFile("package", "capture.zip", "application/zip", packageBytes))
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", "idem-article-0001"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.captureId").value(captureId))

        // Reader HTML is sanitized and asset URLs are rewritten to the API.
        val reader = mockMvc.perform(
            get("/api/v1/content-items/$contentItemId/reader").header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contentItem.title").value("Example article"))
            .andExpect(jsonPath("$.artifact.id").value(artifactId))
            .andReturn()

        val html = objectMapper.readTree(reader.response.contentAsString).get("html").asText()
        assertThat(html).doesNotContain("<script")
        assertThat(html).doesNotContain("onclick")
        assertThat(html).contains("/api/v1/artifacts/$artifactId/assets/asset-1")
        assertThat(html).doesNotContain("assets.cul.invalid")

        // The stored asset is served with its validated MIME type.
        val asset = mockMvc.perform(
            get("/api/v1/artifacts/$artifactId/assets/asset-1").header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andReturn()
        assertThat(asset.response.contentType).isEqualTo("image/png")
        assertThat(asset.response.contentAsByteArray).isEqualTo(png)

        // The manifest is available and lists asset URLs.
        mockMvc.perform(get("/api/v1/artifacts/$artifactId/manifest").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.assets[0].assetKey").value("asset-1"))
            .andExpect(jsonPath("$.assets[0].url").value("/api/v1/artifacts/$artifactId/assets/asset-1"))
    }

    @Test
    fun `rejects a package whose asset bytes do not match the declared type`() {
        val token = accessToken("tampered@example.com")
        val packageBytes = buildPackage(
            html = "<article><p>Tampered</p></article>",
            text = "Tampered",
            assets = listOf(Asset("asset-1", "image/png", "not actually a png".toByteArray())),
        )

        mockMvc.perform(
            multipart("/api/v1/captures")
                .file(MockMultipartFile("package", "capture.zip", "application/zip", packageBytes))
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", "idem-tampered-0001"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("CAPTURE_INVALID_PACKAGE"))
    }

    @Test
    fun `accepts a capture with missing assets and reports a warning`() {
        val token = accessToken("missing@example.com")
        val packageBytes = buildPackage(
            html = "<article><p>Text</p><img src=\"https://assets.cul.invalid/gone\" alt=\"gone\"></article>",
            text = "Text",
            assets = emptyList(),
        )

        mockMvc.perform(
            multipart("/api/v1/captures")
                .file(MockMultipartFile("package", "capture.zip", "application/zip", packageBytes))
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", "idem-missing-0001"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.warnings[?(@.code == 'ASSET_MISSING')]").exists())
    }

    @Test
    fun `does not expose another owner's content`() {
        val owner = accessToken("owner@example.com")
        val intruder = accessToken("intruder@example.com")
        val packageBytes = buildPackage(
            html = "<article><p>Private</p></article>",
            text = "Private",
            assets = emptyList(),
        )
        val created = mockMvc.perform(
            multipart("/api/v1/captures")
                .file(MockMultipartFile("package", "capture.zip", "application/zip", packageBytes))
                .header("Authorization", "Bearer $owner")
                .header("Idempotency-Key", "idem-owner-0001"),
        ).andExpect(status().isCreated).andReturn()
        val contentItemId = objectMapper.readTree(created.response.contentAsString).get("contentItemId").asInt()

        mockMvc.perform(
            get("/api/v1/content-items/$contentItemId/reader").header("Authorization", "Bearer $intruder"),
        ).andExpect(status().isNotFound)
    }

    private fun accessToken(email: String): String {
        createUser(email)
        val result = mockMvc.perform(
            post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$VALID_PASSWORD"}"""),
        ).andExpect(status().isOk).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    private data class Asset(
        val key: String,
        val mimeType: String,
        val bytes: ByteArray,
    )

    private fun buildPackage(html: String, text: String, assets: List<Asset>): ByteArray {
        val assetJson = assets.joinToString(",") { asset ->
            val checksum = sha256(asset.bytes)
            """{"assetKey":"${asset.key}","mimeType":"${asset.mimeType}","originalUrl":"https://example.com/${asset.key}","altText":"alt","byteSize":${asset.bytes.size},"checksum":"$checksum"}"""
        }
        val manifest = """
            {
              "schemaVersion": 1,
              "source": {
                "url": "https://example.com/article",
                "canonicalUrl": "https://example.com/article",
                "sourceName": "Example",
                "adapterId": "generic-readability",
                "adapterVersion": "1.0.0"
              },
              "artifact": {
                "type": "ARTICLE_READER",
                "title": "Example article",
                "language": "en",
                "readingTimeMinutes": 3
              },
              "metadata": { "author": "Author", "description": "Description" },
              "assets": [ $assetJson ],
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
            zip.write(text.toByteArray())
            zip.closeEntry()
            assets.forEach { asset ->
                zip.putNextEntry(ZipEntry("assets/${asset.key}"))
                zip.write(asset.bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun onePixelPng(): ByteArray =
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
        )
}
