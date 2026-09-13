package com.bagusna.catchuplater.features.library.repository

import com.bagusna.catchuplater.features.library.domain.ContentItemTag
import org.springframework.data.jpa.repository.JpaRepository

interface ContentItemTagRepository : JpaRepository<ContentItemTag, Int> {
    fun findAllByContentItemId(contentItemId: Int): List<ContentItemTag>

    fun findAllByTagId(tagId: Int): List<ContentItemTag>

    fun findAllByContentItemIdIn(contentItemIds: Collection<Int>): List<ContentItemTag>

    fun findByContentItemIdAndTagId(contentItemId: Int, tagId: Int): ContentItemTag?

    fun deleteAllByContentItemId(contentItemId: Int)

    fun deleteAllByTagId(tagId: Int)
}
