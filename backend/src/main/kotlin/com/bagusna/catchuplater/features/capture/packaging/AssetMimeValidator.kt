package com.bagusna.catchuplater.features.capture.packaging

import org.springframework.stereotype.Component

/**
 * Validates that a declared asset MIME type is allowed and that the file's
 * magic bytes actually match it. Declared types are never trusted on their
 * own.
 */
@Component
class AssetMimeValidator {
    fun isAllowed(mimeType: String): Boolean = mimeType.lowercase() in ALLOWED

    /**
     * Returns true when [signature] (the first bytes of the asset) matches the
     * declared [mimeType].
     */
    fun matchesSignature(mimeType: String, signature: ByteArray): Boolean = when (mimeType.lowercase()) {
        "image/png" -> startsWith(signature, PNG_MAGIC)
        "image/jpeg" -> startsWith(signature, JPEG_MAGIC)
        "image/gif" -> startsWith(signature, GIF_MAGIC)
        "image/webp" -> signature.size >= 12 &&
            String(signature, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(signature, 8, 4, Charsets.US_ASCII) == "WEBP"
        else -> false
    }

    private fun startsWith(signature: ByteArray, magic: ByteArray): Boolean {
        if (signature.size < magic.size) return false
        return magic.indices.all { signature[it] == magic[it] }
    }

    companion object {
        /** SVG is intentionally excluded: it can carry executable script. */
        val ALLOWED: Set<String> = setOf("image/png", "image/jpeg", "image/gif", "image/webp")

        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val GIF_MAGIC = byteArrayOf(0x47, 0x49, 0x46, 0x38)
    }
}
