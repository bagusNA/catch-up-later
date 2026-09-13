package com.bagusna.catchuplater.features.content.repository

import com.bagusna.catchuplater.features.content.domain.ArtifactAsset
import org.springframework.data.jpa.repository.JpaRepository

interface ArtifactAssetRepository : JpaRepository<ArtifactAsset, Int> {
    fun findByArtifactVersionIdOrderByAssetKey(artifactVersionId: Int): List<ArtifactAsset>

    fun findByArtifactVersionIdAndAssetKey(artifactVersionId: Int, assetKey: String): ArtifactAsset?

    fun deleteAllByArtifactVersionId(artifactVersionId: Int)
}
