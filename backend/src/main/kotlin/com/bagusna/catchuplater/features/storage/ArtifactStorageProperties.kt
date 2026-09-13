package com.bagusna.catchuplater.features.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

/**
 * Filesystem locations for immutable artifact storage and the staging area.
 *
 * Paths are resolved to absolute paths at startup so that later symlink or
 * working-directory changes cannot silently redirect writes.
 */
@ConfigurationProperties(prefix = "app.storage")
data class ArtifactStorageProperties(
    val artifactsDir: Path = Path.of("./data/artifacts"),
    val stagingDir: Path = Path.of("./data/staging"),
) {
    fun resolvedArtifactsDir(): Path = artifactsDir.toAbsolutePath().normalize()

    fun resolvedStagingDir(): Path = stagingDir.toAbsolutePath().normalize()
}
