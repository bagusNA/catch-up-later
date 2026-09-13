package com.bagusna.catchuplater.features.capture.packaging

/** Fixed entry names in a capture package schema v1 ZIP archive. */
object CapturePackagePaths {
    const val MANIFEST = "manifest.json"
    const val HTML = "content.html"
    const val TEXT = "content.txt"
    const val ASSETS_PREFIX = "assets/"

    /**
     * Images inside `content.html` are rewritten to this reserved, never
     * resolvable host before upload. The backend sanitizer only preserves
     * image sources on this host and the reader rewrites them to
     * ownership-checked asset endpoints.
     */
    const val RESERVED_ASSET_HOST = "assets.cul.invalid"
    const val RESERVED_ASSET_PREFIX = "https://$RESERVED_ASSET_HOST/"

    val ASSET_KEY_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
}
