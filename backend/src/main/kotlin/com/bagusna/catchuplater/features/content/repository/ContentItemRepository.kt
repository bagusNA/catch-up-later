package com.bagusna.catchuplater.features.content.repository

import com.bagusna.catchuplater.features.content.domain.ContentItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ContentItemRepository : JpaRepository<ContentItem, Int> {
    fun findByIdAndOwnerIdAndDeletedAtIsNull(id: Int, ownerId: Int): ContentItem?

    fun findAllByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId: Int, pageable: Pageable): Page<ContentItem>
}
