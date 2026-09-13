package com.bagusna.catchuplater.features.capture.packaging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

/**
 * Capture package schema version 1 manifest (`manifest.json`).
 *
 * The manifest is the versioned contract between the extension and the
 * backend. Binary payloads live in sibling ZIP entries:
 *
 * ```text
 * manifest.json
 * content.html
 * content.txt
 * assets/{assetKey}
 * ```
 *
 * Unknown fields are ignored for forward compatibility. Unsupported
 * [schemaVersion]s are rejected during validation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureManifest(
    val schemaVersion: Int = 0,
    val source: CaptureSource = CaptureSource(),
    val artifact: CaptureArtifact = CaptureArtifact(),
    val metadata: CaptureMetadata = CaptureMetadata(),
    val assets: List<CaptureAssetReference> = emptyList(),
    val capture: CaptureInfo = CaptureInfo(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureSource(
    val url: String? = null,
    val canonicalUrl: String? = null,
    val sourceName: String? = null,
    val adapterId: String? = null,
    val adapterVersion: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureArtifact(
    val type: String? = null,
    val title: String? = null,
    val language: String? = null,
    val readingTimeMinutes: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureMetadata(
    val author: String? = null,
    val description: String? = null,
    val siteName: String? = null,
    val publishedAt: Instant? = null,
    val image: CaptureImageReference? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureImageReference(
    val assetKey: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureAssetReference(
    val assetKey: String? = null,
    val mimeType: String? = null,
    val originalUrl: String? = null,
    val altText: String? = null,
    val byteSize: Long? = null,
    val checksum: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureInfo(
    val capturedAt: Instant? = null,
    val warnings: List<CaptureWarning> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureWarning(
    val code: String = "UNKNOWN",
    val message: String = "",
)
