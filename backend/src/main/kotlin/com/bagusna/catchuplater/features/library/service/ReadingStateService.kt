package com.bagusna.catchuplater.features.library.service

import com.bagusna.catchuplater.common.error.InvalidReadingStateException
import com.bagusna.catchuplater.common.error.ResourceNotFoundException
import com.bagusna.catchuplater.features.content.dto.PositionDto
import com.bagusna.catchuplater.features.content.dto.ReadingStateResponse
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.library.domain.ReadingState
import com.bagusna.catchuplater.features.library.domain.ReadingStatus
import com.bagusna.catchuplater.features.library.repository.ReadingStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Reading progress and status. A row is created on first write; an absent row
 * means `UNREAD`. Capture processing status is never touched here (DEC-006).
 */
@Service
class ReadingStateService(
    private val readingStates: ReadingStateRepository,
    private val contentItems: ContentItemRepository,
) {
    @Transactional(readOnly = true)
    fun get(ownerId: Int, contentItemId: Int): ReadingStateResponse {
        requireOwnedItem(ownerId, contentItemId)
        val state = readingStates.findByContentItemIdAndOwnerId(contentItemId, ownerId)
        return state.toResponse(contentItemId)
    }

    @Transactional
    fun update(
        ownerId: Int,
        contentItemId: Int,
        status: String?,
        progressPercent: Double?,
        position: PositionDto?,
        markReadAtEnd: Boolean = false,
    ): ReadingStateResponse {
        val item = requireOwnedItem(ownerId, contentItemId)
        val state = readingStates.findByContentItemIdAndOwnerId(contentItemId, ownerId)
            ?: ReadingState(ownerId = ownerId, contentItemId = contentItemId)

        val requestedStatus = status?.trim()?.takeIf { it.isNotEmpty() }?.let { parseStatus(it) }
        if (progressPercent != null && progressPercent !in 0.0..100.0) {
            throw InvalidReadingStateException("progressPercent must be between 0 and 100.")
        }
        position?.let { validatePosition(it) }

        requestedStatus?.let { state.status = it }
        progressPercent?.let { state.progressPercent = it }
        position?.let {
            state.positionType = it.type?.trim()?.takeIf { type -> type.isNotEmpty() }
            state.positionValue = it.value
        }

        // Derive a status only when the caller did not set one explicitly.
        if (requestedStatus == null) {
            val progress = progressPercent ?: state.progressPercent
            when {
                markReadAtEnd && state.status != ReadingStatus.READ -> state.status = ReadingStatus.READ
                progress != null && progress >= 100.0 -> state.status = ReadingStatus.READ
                progress != null && progress >= MEANINGFUL_PROGRESS_PERCENT &&
                    state.status == ReadingStatus.UNREAD -> state.status = ReadingStatus.IN_PROGRESS
            }
        }

        val now = Instant.now()
        state.lastReadAt = now
        val saved = readingStates.saveAndFlush(state)
        item.lastReadAt = now
        contentItems.save(item)
        return saved.toResponse(contentItemId)
    }

    private fun requireOwnedItem(ownerId: Int, contentItemId: Int) =
        contentItems.findByIdAndOwnerIdAndDeletedAtIsNull(contentItemId, ownerId)
            ?: throw ResourceNotFoundException("The content item was not found.")

    private fun parseStatus(value: String): ReadingStatus =
        runCatching { ReadingStatus.valueOf(value.uppercase()) }
            .getOrElse {
                throw InvalidReadingStateException(
                    "status must be one of UNREAD, IN_PROGRESS or READ.",
                )
            }

    private fun validatePosition(position: PositionDto) {
        val type = position.type?.trim()
        if (type != null && type.length > 32) {
            throw InvalidReadingStateException("position.type must be at most 32 characters.")
        }
        if (position.value != null && position.value < 0) {
            throw InvalidReadingStateException("position.value must not be negative.")
        }
    }

    private fun ReadingState?.toResponse(contentItemId: Int): ReadingStateResponse =
        if (this == null) {
            ReadingStateResponse(
                contentItemId = contentItemId,
                status = ReadingStatus.UNREAD.name,
                progressPercent = null,
                position = null,
                lastReadAt = null,
                updatedAt = null,
            )
        } else {
            ReadingStateResponse(
                contentItemId = contentItemId,
                status = status.name,
                progressPercent = progressPercent,
                position = positionType?.let { PositionDto(it, positionValue) },
                lastReadAt = lastReadAt,
                updatedAt = updatedAt,
            )
        }

    companion object {
        /** Progress at or above this percentage counts as "meaningful". */
        const val MEANINGFUL_PROGRESS_PERCENT = 1.0
    }
}
