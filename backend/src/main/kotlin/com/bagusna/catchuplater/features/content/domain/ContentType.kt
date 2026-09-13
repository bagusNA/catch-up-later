package com.bagusna.catchuplater.features.content.domain

/**
 * The reader/storage family of a captured content item. `ARTICLE` is produced
 * by the generic Readability adapter in `S2`; `PDF` arrives in `S7`.
 */
enum class ContentType {
    ARTICLE,
    PDF,
}
