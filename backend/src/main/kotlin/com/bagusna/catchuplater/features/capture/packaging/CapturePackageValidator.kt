package com.bagusna.catchuplater.features.capture.packaging

import com.bagusna.catchuplater.common.error.InvalidCapturePackageException
import com.bagusna.catchuplater.common.error.UnsupportedArtifactTypeException
import com.bagusna.catchuplater.features.content.domain.ArtifactType
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant

data class ValidatedAsset(
    val assetKey: String,
    val mimeType: String,
    val originalUrl: String?,
    val altText: String?,
    val byteSize: Long,
    val checksum: String,
    val width: Int?,
    val height: Int?,
    val relativePath: String,
)

data class ValidatedCapturePackage(
    val schemaVersion: Int,
    val sourceUrl: String,
    val canonicalUrl: String?,
    val sourceName: String?,
    val adapterId: String,
    val adapterVersion: String,
    val artifactType: ArtifactType,
    val title: String,
    val language: String?,
    val readingTimeMinutes: Int?,
    val author: String?,
    val description: String?,
    val siteName: String?,
    val publishedAt: Instant?,
    val html: String,
    val text: String?,
    val assets: List<ValidatedAsset>,
    val capturedAt: Instant,
    val warnings: List<CaptureWarning>,
)

/**
 * Semantic validation of a package that has already been streamed to staging.
 *
 * Every failure is fail-closed and produces a stable error code. Declaration
 * values (MIME type, size, checksum) are cross-checked against the bytes the
 * backend actually received.
 */
@Component
class CapturePackageValidator(
    private val mimeValidator: AssetMimeValidator,
    private val properties: CapturePackageProperties,
) {
    fun validate(received: ReceivedCapturePackage): ValidatedCapturePackage {
        val manifest = received.manifest

        if (manifest.schemaVersion !in properties.supportedSchemaVersions) {
            throw InvalidCapturePackageException(
                "Unsupported capture package schema version ${manifest.schemaVersion}.",
                mapOf("schemaVersion" to manifest.schemaVersion, "supported" to properties.supportedSchemaVersions),
            )
        }

        val artifactType = when (manifest.artifact.type) {
            ArtifactType.ARTICLE_READER.name -> ArtifactType.ARTICLE_READER
            ArtifactType.PDF_DOCUMENT.name ->
                throw UnsupportedArtifactTypeException("PDF capture is not supported yet.")
            else -> throw InvalidCapturePackageException("Unknown artifact type '${manifest.artifact.type}'.")
        }

        val sourceUrl = requireUrl(manifest.source.url, "source.url")
        val canonicalUrl = manifest.source.canonicalUrl?.let { normalizeUrl(it) }
        val title = normalizeText(manifest.artifact.title, properties.maxTitleLength)
            ?: throw InvalidCapturePackageException("The package is missing an article title.")

        val html = received.html
            ?: throw InvalidCapturePackageException("The package is missing ${CapturePackagePaths.HTML}.")

        val warnings = received.warnings.toMutableList()

        val validatedAssets = validateAssets(manifest, received, warnings)

        val text = received.text?.takeIf { it.isNotBlank() }
        val readingTime = manifest.artifact.readingTimeMinutes?.takeIf { it > 0 }
            ?: text?.let { estimateReadingTime(it) }

        return ValidatedCapturePackage(
            schemaVersion = manifest.schemaVersion,
            sourceUrl = sourceUrl,
            canonicalUrl = canonicalUrl,
            sourceName = normalizeText(manifest.source.sourceName, MAX_NAME_LENGTH),
            adapterId = normalizeText(manifest.source.adapterId, MAX_ADAPTER_LENGTH) ?: UNKNOWN_ADAPTER,
            adapterVersion = normalizeText(manifest.source.adapterVersion, MAX_ADAPTER_LENGTH) ?: UNKNOWN_ADAPTER,
            artifactType = artifactType,
            title = title,
            language = normalizeText(manifest.artifact.language, MAX_LANGUAGE_LENGTH),
            readingTimeMinutes = readingTime,
            author = normalizeText(manifest.metadata.author, MAX_AUTHOR_LENGTH),
            description = normalizeText(manifest.metadata.description, properties.maxDescriptionLength),
            siteName = normalizeText(manifest.metadata.siteName, MAX_NAME_LENGTH),
            publishedAt = manifest.metadata.publishedAt,
            html = html,
            text = text,
            assets = validatedAssets,
            capturedAt = manifest.capture.capturedAt ?: Instant.now(),
            warnings = warnings,
        )
    }

    private fun validateAssets(
        manifest: CaptureManifest,
        received: ReceivedCapturePackage,
        warnings: MutableList<CaptureWarning>,
    ): List<ValidatedAsset> {
        val declaredKeys = mutableSetOf<String>()
        val validated = mutableListOf<ValidatedAsset>()

        for (reference in manifest.assets) {
            val key = reference.assetKey
                ?: throw InvalidCapturePackageException("An asset reference is missing its assetKey.")
            if (!CapturePackagePaths.ASSET_KEY_PATTERN.matches(key)) {
                throw InvalidCapturePackageException("Asset key '$key' is not valid.")
            }
            if (!declaredKeys.add(key)) {
                throw InvalidCapturePackageException("Asset key '$key' is declared more than once.")
            }

            val staged = received.assets[key]
                ?: throw InvalidCapturePackageException("Asset '$key' is declared in the manifest but missing from the package.")

            val mimeType = reference.mimeType?.lowercase()
                ?: throw InvalidCapturePackageException("Asset '$key' is missing its mimeType.")
            if (!mimeValidator.isAllowed(mimeType)) {
                throw InvalidCapturePackageException("Asset '$key' has a disallowed MIME type '$mimeType'.")
            }
            if (!mimeValidator.matchesSignature(mimeType, staged.signature)) {
                throw InvalidCapturePackageException("Asset '$key' does not match its declared MIME type.")
            }
            reference.byteSize?.let {
                if (it != staged.byteSize) {
                    throw InvalidCapturePackageException("Asset '$key' size does not match the manifest.")
                }
            }
            reference.checksum?.let {
                if (!it.equals(staged.checksum, ignoreCase = true)) {
                    throw InvalidCapturePackageException("Asset '$key' checksum does not match the manifest.")
                }
            }

            validated.add(
                ValidatedAsset(
                    assetKey = key,
                    mimeType = mimeType,
                    originalUrl = normalizeInformationalUrl(reference.originalUrl),
                    altText = normalizeText(reference.altText, MAX_ALT_LENGTH),
                    byteSize = staged.byteSize,
                    checksum = staged.checksum,
                    width = reference.width?.takeIf { it > 0 },
                    height = reference.height?.takeIf { it > 0 },
                    relativePath = staged.relativePath,
                ),
            )
        }

        val undeclared = received.assets.keys - declaredKeys
        if (undeclared.isNotEmpty()) {
            warnings.add(
                CaptureWarning(
                    code = "UNDECLARED_ASSET",
                    message = "Ignored ${undeclared.size} asset(s) that were not declared in the manifest.",
                ),
            )
        }
        return validated
    }

    private fun requireUrl(value: String?, field: String): String {
        val normalized = normalizeUrl(value)
            ?: throw InvalidCapturePackageException("The package is missing $field.")
        return normalized
    }

    private fun normalizeUrl(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.length > properties.maxUrlLength) {
            throw InvalidCapturePackageException("A URL in the package exceeds the maximum length.")
        }
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            throw InvalidCapturePackageException("A URL in the package is not valid.")
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw InvalidCapturePackageException("A URL in the package uses an unsupported scheme.")
        }
        if (uri.host.isNullOrBlank()) {
            throw InvalidCapturePackageException("A URL in the package is not valid.")
        }
        return trimmed
    }

    /**
     * Asset `originalUrl` is informational only (it is never fetched), so it is
     * kept verbatim apart from trimming and truncation. This tolerates
     * `data:` URLs without opening an SSRF surface.
     */
    private fun normalizeInformationalUrl(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return trimmed.take(properties.maxUrlLength)
    }

    private fun normalizeText(value: String?, maxLength: Int): String? {
        val cleaned = value
            ?.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return cleaned.take(maxLength)
    }

    private fun estimateReadingTime(text: String): Int {
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        return maxOf(1, kotlin.math.ceil(words / WORDS_PER_MINUTE.toDouble()).toInt())
    }

    companion object {
        private const val WORDS_PER_MINUTE = 220
        private const val UNKNOWN_ADAPTER = "unknown"
        private const val MAX_NAME_LENGTH = 255
        private const val MAX_AUTHOR_LENGTH = 300
        private const val MAX_ADAPTER_LENGTH = 100
        private const val MAX_LANGUAGE_LENGTH = 35
        private const val MAX_ALT_LENGTH = 1000
    }
}
