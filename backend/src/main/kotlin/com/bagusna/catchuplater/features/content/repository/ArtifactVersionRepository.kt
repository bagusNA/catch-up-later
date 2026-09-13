package com.bagusna.catchuplater.features.content.repository

import com.bagusna.catchuplater.features.content.domain.ArtifactVersion
import org.springframework.data.jpa.repository.JpaRepository

interface ArtifactVersionRepository : JpaRepository<ArtifactVersion, Int> {
    fun findByContentItemIdAndIsCurrentTrue(contentItemId: Int): ArtifactVersion?

    fun findByContentItemIdOrderByVersionNumberDesc(contentItemId: Int): List<ArtifactVersion>

    fun findByContentItemIdInAndIsCurrentTrue(contentItemIds: Collection<Int>): List<ArtifactVersion>

    fun deleteAllByContentItemId(contentItemId: Int)
}
