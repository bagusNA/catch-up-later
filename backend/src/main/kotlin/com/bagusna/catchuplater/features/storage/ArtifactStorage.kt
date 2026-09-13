package com.bagusna.catchuplater.features.storage

import java.io.InputStream
import java.nio.file.Path

/**
 * Storage abstraction for immutable capture artifacts. The MVP implementation
 * is filesystem-backed, but services depend only on this interface so object
 * storage can be added later without changing domain code.
 */
interface ArtifactStorage {
    /** Creates an empty, private staging area for a single capture. */
    fun createStaging(): StagingArea

    /**
     * Atomically moves a fully written staging area under the generated
     * [storageKey]. Fails if the target already exists.
     */
    fun commit(stagingId: String, storageKey: String)

    /** Removes a staging area that will not be committed. */
    fun discard(stagingId: String)

    fun open(storageKey: String): InputStream

    fun exists(storageKey: String): Boolean

    fun delete(storageKey: String)
}

/**
 * A temporary directory for one capture. Paths passed to [write]/[path] are
 * always relative and validated, so untrusted entry names can never escape the
 * area.
 */
interface StagingArea {
    val id: String

    fun write(relativePath: String, bytes: ByteArray)

    fun write(relativePath: String, stream: InputStream)

    fun path(relativePath: String): Path
}
