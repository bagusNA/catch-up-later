package com.bagusna.catchuplater.features.content.service

import com.bagusna.catchuplater.features.content.domain.ArtifactVersion
import com.bagusna.catchuplater.features.content.dto.ArtifactSummary

/** Maps an immutable artifact version to its API representation. */
fun ArtifactVersion.toArtifactSummary(): ArtifactSummary =
    ArtifactSummary(
        id = requireNotNull(id),
        versionNumber = versionNumber,
        isCurrent = isCurrent,
        artifactType = artifactType.name,
        checksum = checksum,
        byteSize = byteSize,
        readingTimeMinutes = readingTimeMinutes,
        capturedAt = capturedAt,
        adapterId = adapterId,
        adapterVersion = adapterVersion,
        packageSchemaVersion = packageSchemaVersion,
        validationStatus = validationStatus.name,
    )
