package com.bagusna.catchuplater.features.capture.packaging

import com.bagusna.catchuplater.common.error.CaptureTooLargeException
import com.bagusna.catchuplater.common.error.InvalidCapturePackageException
import com.bagusna.catchuplater.features.storage.StagingArea
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.io.FilterInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * A single asset as read from the package and written to staging. The
 * authoritative size, checksum, and signature are computed by the backend;
 * values declared in the manifest are only cross-checked.
 */
data class StagedAsset(
    val assetKey: String,
    val relativePath: String,
    val byteSize: Long,
    val checksum: String,
    val signature: ByteArray,
)

/** Everything read out of a package, before semantic validation. */
data class ReceivedCapturePackage(
    val manifest: CaptureManifest,
    val html: String?,
    val text: String?,
    val assets: Map<String, StagedAsset>,
    val warnings: MutableList<CaptureWarning> = mutableListOf(),
)

/**
 * Streams a capture package ZIP into a staging area, enforcing structural
 * limits as it goes.
 *
 * The archive is never fully buffered: text entries are bounded reads and
 * binary assets are streamed straight to disk while their size, checksum and
 * signature are computed. Untrusted entry names are validated here so they can
 * never become filesystem paths.
 */
@Component
class CapturePackageReader(
    private val objectMapper: ObjectMapper,
    private val properties: CapturePackageProperties,
) {
    fun read(staging: StagingArea, packageStream: InputStream): ReceivedCapturePackage {
        val counting = CountingInputStream(packageStream)
        val assets = linkedMapOf<String, StagedAsset>()
        var totalAssetBytes = 0L
        var manifestBytes: ByteArray? = null
        var html: String? = null
        var text: String? = null
        val warnings = mutableListOf<CaptureWarning>()
        val seenEntries = mutableSetOf<String>()

        ZipInputStream(counting).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = safeEntryName(entry)
                if (!seenEntries.add(name)) {
                    throw InvalidCapturePackageException("The package contains duplicate entry '$name'.")
                }
                ensurePackageWithinLimit(counting.count)

                when {
                    name == CapturePackagePaths.MANIFEST ->
                        manifestBytes = readBounded(zip, properties.maxManifestBytes, "manifest.json")

                    name == CapturePackagePaths.HTML ->
                        html = String(readBounded(zip, properties.maxHtmlBytes, "content.html"), Charsets.UTF_8)

                    name == CapturePackagePaths.TEXT ->
                        text = String(readBounded(zip, properties.maxTextBytes, "content.txt"), Charsets.UTF_8)

                    name.startsWith(CapturePackagePaths.ASSETS_PREFIX) -> {
                        val key = name.removePrefix(CapturePackagePaths.ASSETS_PREFIX)
                        if (assets.size >= properties.maxAssetCount) {
                            throw com.bagusna.catchuplater.common.error.AssetLimitExceededException(
                                "The package contains more than ${properties.maxAssetCount} assets.",
                            )
                        }
                        val staged = readAsset(staging, key, zip)
                        totalAssetBytes += staged.byteSize
                        if (totalAssetBytes > properties.maxTotalAssetBytes) {
                            throw com.bagusna.catchuplater.common.error.AssetLimitExceededException(
                                "The package assets exceed the total size limit.",
                            )
                        }
                        assets[key] = staged
                    }

                    else -> warnings.add(
                        CaptureWarning(
                            code = "UNRECOGNIZED_ENTRY",
                            message = "Ignored unrecognized package entry '$name'.",
                        ),
                    )
                }
                zip.closeEntry()
            }
        }

        val manifestJson = manifestBytes
            ?: throw InvalidCapturePackageException("The package is missing manifest.json.")
        val manifest = try {
            objectMapper.readValue(manifestJson, CaptureManifest::class.java)
        } catch (exception: Exception) {
            throw InvalidCapturePackageException("The package manifest is not valid JSON.")
        }

        return ReceivedCapturePackage(
            manifest = manifest,
            html = html,
            text = text,
            assets = assets,
            warnings = warnings,
        )
    }

    private fun readAsset(staging: StagingArea, key: String, zip: ZipInputStream): StagedAsset {
        if (!CapturePackagePaths.ASSET_KEY_PATTERN.matches(key)) {
            throw InvalidCapturePackageException("The package contains an invalid asset key.")
        }
        val relativePath = "${CapturePackagePaths.ASSETS_PREFIX}$key"
        val target = staging.path(relativePath)
        target.parent?.let { java.nio.file.Files.createDirectories(it) }

        val digest = MessageDigest.getInstance("SHA-256")
        val signature = ByteArray(SIGNATURE_BYTES)
        var signatureLength = 0
        var total = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        java.nio.file.Files.newOutputStream(target).use { output ->
            while (true) {
                val read = zip.read(buffer)
                if (read < 0) break
                total += read
                if (total > properties.maxAssetBytes) {
                    throw CaptureTooLargeException("Asset '$key' exceeds the maximum asset size.")
                }
                digest.update(buffer, 0, read)
                if (signatureLength < SIGNATURE_BYTES) {
                    val copy = minOf(SIGNATURE_BYTES - signatureLength, read)
                    System.arraycopy(buffer, 0, signature, signatureLength, copy)
                    signatureLength += copy
                }
                output.write(buffer, 0, read)
            }
        }

        if (total == 0L) {
            throw InvalidCapturePackageException("Asset '$key' is empty.")
        }

        return StagedAsset(
            assetKey = key,
            relativePath = relativePath,
            byteSize = total,
            checksum = digest.digest().toHex(),
            signature = signature.copyOf(signatureLength),
        )
    }

    private fun readBounded(input: InputStream, limit: Long, label: String): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            total += read
            if (total > limit) {
                throw CaptureTooLargeException("Entry '$label' exceeds the maximum allowed size.")
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun ensurePackageWithinLimit(bytesRead: Long) {
        if (bytesRead > properties.maxPackageBytes) {
            throw CaptureTooLargeException("The package exceeds the maximum allowed size.")
        }
    }

    private fun safeEntryName(entry: ZipEntry): String {
        val name = entry.name
        if (name.isBlank() || name.contains('\\') || name.contains('\u0000')) {
            throw InvalidCapturePackageException("The package contains an unsafe entry name.")
        }
        if (name.startsWith("/") || name.startsWith("~")) {
            throw InvalidCapturePackageException("The package contains an unsafe entry name.")
        }
        val segments = name.split('/')
        if (segments.any { it == ".." || it == "." || it.isEmpty() }) {
            throw InvalidCapturePackageException("The package contains an unsafe entry name.")
        }
        return name
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val SIGNATURE_BYTES = 16
        private val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}

/** Counts bytes pulled from the underlying upload stream. */
private class CountingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
    var count: Long = 0
        private set

    override fun read(): Int {
        val value = super.read()
        if (value != -1) count++
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read > 0) count += read
        return read
    }
}
