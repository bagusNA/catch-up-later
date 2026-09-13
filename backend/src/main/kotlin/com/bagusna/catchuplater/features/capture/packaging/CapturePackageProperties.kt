package com.bagusna.catchuplater.features.capture.packaging

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Hard limits enforced while reading a capture package. Limits fail closed:
 * exceeding any of them produces an `INVALID_PACKAGE`/`CONTENT_TOO_LARGE`
 * error rather than a partially stored artifact.
 */
@ConfigurationProperties(prefix = "app.capture")
data class CapturePackageProperties(
    val supportedSchemaVersions: Set<Int> = setOf(1),
    val maxPackageBytes: Long = 52_428_800,
    val maxManifestBytes: Long = 2_097_152,
    val maxHtmlBytes: Long = 5_242_880,
    val maxTextBytes: Long = 5_242_880,
    val maxAssetCount: Int = 100,
    val maxAssetBytes: Long = 10_485_760,
    val maxTotalAssetBytes: Long = 52_428_800,
    val maxTitleLength: Int = 500,
    val maxDescriptionLength: Int = 2000,
    val maxUrlLength: Int = 2048,
) {
    init {
        require(maxAssetCount > 0) { "app.capture.max-asset-count must be positive" }
        require(maxAssetBytes > 0) { "app.capture.max-asset-bytes must be positive" }
        require(maxPackageBytes > 0) { "app.capture.max-package-bytes must be positive" }
    }
}
