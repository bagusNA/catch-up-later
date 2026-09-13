package com.bagusna.catchuplater.features.library.repository

import com.bagusna.catchuplater.features.library.domain.ReadingState
import org.springframework.data.jpa.repository.JpaRepository

interface ReadingStateRepository : JpaRepository<ReadingState, Int> {
    fun findByContentItemIdAndOwnerId(contentItemId: Int, ownerId: Int): ReadingState?

    fun findAllByContentItemIdIn(contentItemIds: Collection<Int>): List<ReadingState>

    fun deleteByContentItemId(contentItemId: Int)
}
