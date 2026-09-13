package com.bagusna.catchuplater.features.content.domain

/**
 * The stored artifact kind. `ARTICLE_READER` is normalized, sanitized HTML.
 * `PDF_DOCUMENT` is the original PDF bytes.
 */
enum class ArtifactType {
    ARTICLE_READER,
    PDF_DOCUMENT,
}
