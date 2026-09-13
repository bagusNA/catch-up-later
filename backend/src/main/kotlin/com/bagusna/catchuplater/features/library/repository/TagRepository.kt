package com.bagusna.catchuplater.features.library.repository

import com.bagusna.catchuplater.features.library.domain.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Int> {
    fun findAllByOwnerIdOrderByNameAsc(ownerId: Int): List<Tag>

    fun findByIdAndOwnerId(id: Int, ownerId: Int): Tag?

    fun findByOwnerIdAndNormalizedName(ownerId: Int, normalizedName: String): Tag?

    fun countByOwnerId(ownerId: Int): Long
}
