package com.bagusna.catchuplater.features.capture.packaging

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Serializes the normalized, authoritative manifest that is stored alongside
 * the immutable artifact. The stored manifest is derived only from validated
 * values, never from the raw uploaded JSON.
 */
@Component
class CaptureManifestWriter(
    private val objectMapper: ObjectMapper,
) {
    fun write(validated: ValidatedCapturePackage, warnings: List<CaptureWarning>): ByteArray {
        val manifest = linkedMapOf<String, Any?>(
            "schemaVersion" to validated.schemaVersion,
            "source" to linkedMapOf(
                "url" to validated.sourceUrl,
                "canonicalUrl" to validated.canonicalUrl,
                "sourceName" to validated.sourceName,
                "adapterId" to validated.adapterId,
                "adapterVersion" to validated.adapterVersion,
            ),
            "artifact" to linkedMapOf(
                "type" to validated.artifactType.name,
                "title" to validated.title,
                "html" to CapturePackagePaths.HTML,
                "text" to if (validated.text != null) CapturePackagePaths.TEXT else null,
                "language" to validated.language,
                "readingTimeMinutes" to validated.readingTimeMinutes,
            ),
            "metadata" to linkedMapOf(
                "author" to validated.author,
                "description" to validated.description,
                "siteName" to validated.siteName,
                "publishedAt" to validated.publishedAt?.toString(),
            ),
            "assets" to validated.assets.map { asset ->
                linkedMapOf(
                    "assetKey" to asset.assetKey,
                    "mimeType" to asset.mimeType,
                    "originalUrl" to asset.originalUrl,
                    "altText" to asset.altText,
                    "byteSize" to asset.byteSize,
                    "checksum" to asset.checksum,
                    "width" to asset.width,
                    "height" to asset.height,
                )
            },
            "capture" to linkedMapOf(
                "capturedAt" to validated.capturedAt.toString(),
                "warnings" to warnings.map { linkedMapOf("code" to it.code, "message" to it.message) },
            ),
        )
        return objectMapper.writeValueAsBytes(manifest)
    }
}
