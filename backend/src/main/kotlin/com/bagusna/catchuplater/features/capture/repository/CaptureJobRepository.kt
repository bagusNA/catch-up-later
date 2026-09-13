package com.bagusna.catchuplater.features.capture.repository

import com.bagusna.catchuplater.features.capture.domain.CaptureJob
import org.springframework.data.jpa.repository.JpaRepository

interface CaptureJobRepository : JpaRepository<CaptureJob, Int> {
    fun findByPublicIdAndOwnerId(publicId: String, ownerId: Int): CaptureJob?

    fun findByOwnerIdAndIdempotencyKey(ownerId: Int, idempotencyKey: String): CaptureJob?
}
