package com.bagusna.catchuplater.features.storage

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Filesystem-backed [ArtifactStorage].
 *
 * Layout:
 * ```text
 * {artifactsDir}/{ownerId}/{contentItemId}/{versionNumber}/
 * {stagingDir}/{stagingId}/
 * ```
 *
 * Staging lives outside the artifact tree and is only moved into place on
 * [commit], so readers never observe a partially written artifact.
 */
@Component
class FilesystemArtifactStorage(
    private val properties: ArtifactStorageProperties,
) : ArtifactStorage {

    private val log = LoggerFactory.getLogger(javaClass)
    private val artifactsRoot: Path = properties.resolvedArtifactsDir()
    private val stagingRoot: Path = properties.resolvedStagingDir()

    init {
        Files.createDirectories(artifactsRoot)
        Files.createDirectories(stagingRoot)
    }

    override fun createStaging(): StagingArea {
        val id = UUID.randomUUID().toString()
        val directory = stagingRoot.resolve(id)
        Files.createDirectories(directory)
        return FilesystemStagingArea(id, directory)
    }

    override fun commit(stagingId: String, storageKey: String) {
        val source = resolveSafe(stagingRoot, stagingId)
        val target = resolveSafe(artifactsRoot, storageKey)
        check(Files.isDirectory(source)) { "Staging area $stagingId does not exist" }
        check(!Files.exists(target)) { "Artifact already exists at $storageKey" }
        Files.createDirectories(target.parent)
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
        log.debug("Committed artifact {} from staging {}", storageKey, stagingId)
    }

    override fun discard(stagingId: String) {
        val directory = resolveSafe(stagingRoot, stagingId)
        deleteRecursively(directory)
    }

    override fun open(storageKey: String): InputStream {
        val path = resolveSafe(artifactsRoot, storageKey)
        check(Files.isRegularFile(path)) { "Artifact file is not available" }
        return Files.newInputStream(path)
    }

    override fun exists(storageKey: String): Boolean =
        Files.exists(resolveSafe(artifactsRoot, storageKey))

    override fun delete(storageKey: String) {
        deleteRecursively(resolveSafe(artifactsRoot, storageKey))
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun resolveSafe(root: Path, relative: String): Path {
        require(relative.isNotBlank()) { "Path must not be blank" }
        require(!relative.contains('\\')) { "Path must not contain backslashes" }
        val resolved = root.resolve(relative).normalize()
        require(resolved.startsWith(root)) { "Path escapes the storage root" }
        return resolved
    }
}

private class FilesystemStagingArea(
    override val id: String,
    private val directory: Path,
) : StagingArea {

    override fun write(relativePath: String, bytes: ByteArray) {
        val target = resolve(relativePath)
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, ".${target.fileName}", ".tmp")
        Files.write(temporary, bytes)
        moveIntoPlace(temporary, target)
    }

    override fun write(relativePath: String, stream: InputStream) {
        val target = resolve(relativePath)
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, ".${target.fileName}", ".tmp")
        Files.newOutputStream(temporary).use { output -> stream.copyTo(output) }
        moveIntoPlace(temporary, target)
    }

    override fun path(relativePath: String): Path = resolve(relativePath)

    private fun moveIntoPlace(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun resolve(relativePath: String): Path {
        require(relativePath.isNotBlank()) { "Path must not be blank" }
        require(!relativePath.contains('\\')) { "Path must not contain backslashes" }
        val resolved = directory.resolve(relativePath).normalize()
        require(resolved.startsWith(directory)) { "Path escapes the staging area" }
        return resolved
    }
}
