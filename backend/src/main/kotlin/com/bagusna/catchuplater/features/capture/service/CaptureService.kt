package com.bagusna.catchuplater.features.capture.service

import com.bagusna.catchuplater.common.error.ApiException
import com.bagusna.catchuplater.common.error.CaptureNotFoundException
import com.bagusna.catchuplater.common.error.InvalidIdempotencyKeyException
import com.bagusna.catchuplater.features.capture.domain.CaptureJob
import com.bagusna.catchuplater.features.capture.domain.CaptureStatus
import com.bagusna.catchuplater.features.capture.dto.CaptureErrorDto
import com.bagusna.catchuplater.features.capture.dto.CaptureResponse
import com.bagusna.catchuplater.features.capture.dto.CaptureWarningDto
import com.bagusna.catchuplater.features.capture.packaging.ArticleHtmlSanitizer
import com.bagusna.catchuplater.features.capture.packaging.CaptureManifestWriter
import com.bagusna.catchuplater.features.capture.packaging.CapturePackagePaths
import com.bagusna.catchuplater.features.capture.packaging.CapturePackageReader
import com.bagusna.catchuplater.features.capture.packaging.CapturePackageValidator
import com.bagusna.catchuplater.features.capture.packaging.CaptureWarning
import com.bagusna.catchuplater.features.capture.packaging.SanitizedArticle
import com.bagusna.catchuplater.features.capture.packaging.ValidatedCapturePackage
import com.bagusna.catchuplater.features.capture.repository.CaptureJobRepository
import com.bagusna.catchuplater.features.content.domain.ArtifactAsset
import com.bagusna.catchuplater.features.content.domain.ArtifactType
import com.bagusna.catchuplater.features.content.domain.ArtifactVersion
import com.bagusna.catchuplater.features.content.domain.ContentItem
import com.bagusna.catchuplater.features.content.domain.ContentStatus
import com.bagusna.catchuplater.features.content.domain.ContentType
import com.bagusna.catchuplater.features.content.domain.ValidationStatus
import com.bagusna.catchuplater.features.content.repository.ArtifactAssetRepository
import com.bagusna.catchuplater.features.content.repository.ArtifactVersionRepository
import com.bagusna.catchuplater.features.content.repository.ContentItemRepository
import com.bagusna.catchuplater.features.library.service.SearchIndexService
import com.bagusna.catchuplater.features.storage.ArtifactStorage
import com.bagusna.catchuplater.features.storage.StagingArea
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.util.UUID

/**
 * Orchestrates capture intake: read -> validate -> sanitize -> store.
 *
 * Intake is synchronous for the MVP, so a successful request already reports
 * `READY`. The capture job is still persisted so clients can poll
 * `GET /captures/{captureId}` and observe failures.
 */
@Service
class CaptureService(
    private val jobs: CaptureJobRepository,
    private val packageReader: CapturePackageReader,
    private val packageValidator: CapturePackageValidator,
    private val sanitizer: ArticleHtmlSanitizer,
    private val manifestWriter: CaptureManifestWriter,
    private val storage: ArtifactStorage,
    private val contentItems: ContentItemRepository,
    private val artifacts: ArtifactVersionRepository,
    private val assets: ArtifactAssetRepository,
    private val searchIndex: SearchIndexService,
    private val objectMapper: ObjectMapper,
) {

    @Transactional(noRollbackFor = [ApiException::class])
    fun intake(ownerId: Int, idempotencyKey: String, file: MultipartFile): CaptureResponse {
        validateIdempotencyKey(idempotencyKey)

        jobs.findByOwnerIdAndIdempotencyKey(ownerId, idempotencyKey)?.let { return it.toResponse() }

        val job = jobs.saveAndFlush(
            CaptureJob(
                ownerId = ownerId,
                publicId = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                status = CaptureStatus.PROCESSING,
            ),
        )

        val staging = storage.createStaging()
        try {
            val received = packageReader.read(staging, file.inputStream)
            val validated = packageValidator.validate(received)
            val sanitized = sanitizer.sanitize(
                validated.html,
                validated.assets.map { it.assetKey }.toSet(),
            )
            val warnings = buildWarnings(validated, sanitized)
            return persist(job, ownerId, staging, validated, sanitized, warnings)
        } catch (exception: ApiException) {
            job.markFailed(exception.code, exception.message ?: "The capture could not be processed.")
            jobs.saveAndFlush(job)
            throw exception
        } catch (exception: Exception) {
            job.markFailed("PROCESSING_FAILED", "The capture could not be processed.")
            jobs.saveAndFlush(job)
            throw exception
        } finally {
            // A successful commit moves staging away; this is a no-op then.
            storage.discard(staging.id)
        }
    }

    @Transactional(readOnly = true)
    fun status(ownerId: Int, captureId: String): CaptureResponse {
        val job = jobs.findByPublicIdAndOwnerId(captureId, ownerId) ?: throw CaptureNotFoundException()
        return job.toResponse()
    }

    private fun persist(
        job: CaptureJob,
        ownerId: Int,
        staging: StagingArea,
        validated: ValidatedCapturePackage,
        sanitized: SanitizedArticle,
        warnings: List<CaptureWarning>,
    ): CaptureResponse {
        val contentItem = contentItems.saveAndFlush(
            ContentItem(
                ownerId = ownerId,
                title = validated.title,
                description = validated.description,
                sourceUrl = validated.sourceUrl,
                canonicalUrl = validated.canonicalUrl,
                sourceName = validated.siteName ?: validated.sourceName,
                contentType = ContentType.ARTICLE,
                status = ContentStatus.PROCESSING,
            ),
        )
        val contentItemId = requireNotNull(contentItem.id)

        val versionNumber = (artifacts.findByContentItemIdOrderByVersionNumberDesc(contentItemId)
            .firstOrNull()?.versionNumber ?: 0) + 1
        val storageKey = "$ownerId/$contentItemId/$versionNumber"

        val htmlBytes = sanitized.html.toByteArray(Charsets.UTF_8)
        val textBytes = validated.text?.toByteArray(Charsets.UTF_8)
        val manifestBytes = manifestWriter.write(validated, warnings)

        staging.write(CapturePackagePaths.HTML, htmlBytes)
        textBytes?.let { staging.write(CapturePackagePaths.TEXT, it) }
        staging.write(CapturePackagePaths.MANIFEST, manifestBytes)

        val checksum = digest(manifestBytes, htmlBytes, textBytes)
        val totalBytes = manifestBytes.size.toLong() + htmlBytes.size + (textBytes?.size ?: 0) +
            validated.assets.sumOf { it.byteSize }

        storage.commit(staging.id, storageKey)

        artifacts.findByContentItemIdAndIsCurrentTrue(contentItemId)?.let {
            it.isCurrent = false
            artifacts.save(it)
        }

        val artifact = artifacts.saveAndFlush(
            ArtifactVersion(
                contentItemId = contentItemId,
                versionNumber = versionNumber,
                artifactType = ArtifactType.ARTICLE_READER,
                storageKey = storageKey,
                manifestStorageKey = "$storageKey/${CapturePackagePaths.MANIFEST}",
                checksum = checksum,
                byteSize = totalBytes,
                readingTimeMinutes = validated.readingTimeMinutes,
                capturedAt = validated.capturedAt,
                adapterId = validated.adapterId,
                adapterVersion = validated.adapterVersion,
                packageSchemaVersion = validated.schemaVersion,
                validationStatus = if (warnings.isEmpty()) ValidationStatus.VALID else ValidationStatus.VALID_WITH_WARNINGS,
                isCurrent = true,
            ),
        )
        val artifactId = requireNotNull(artifact.id)

        validated.assets.forEach { asset ->
            assets.save(
                ArtifactAsset(
                    artifactVersionId = artifactId,
                    assetKey = asset.assetKey,
                    mimeType = asset.mimeType,
                    byteSize = asset.byteSize,
                    checksum = asset.checksum,
                    storageKey = "$storageKey/${asset.relativePath}",
                    originalUrl = asset.originalUrl,
                    altText = asset.altText,
                    width = asset.width,
                    height = asset.height,
                ),
            )
        }
        assets.flush()

        contentItem.status = ContentStatus.READY
        contentItems.saveAndFlush(contentItem)

        val warningsJson = objectMapper.writeValueAsString(warnings.map { CaptureWarningDto(it.code, it.message) })
        job.markReady(contentItemId, artifactId, ArtifactType.ARTICLE_READER, validated.sourceUrl, warningsJson)
        jobs.saveAndFlush(job)

        // Index after the artifact and metadata are durably stored. Indexing
        // failure is non-fatal and leaves the artifact readable.
        searchIndex.index(contentItemId)

        return job.toResponse()
    }

    private fun buildWarnings(
        validated: ValidatedCapturePackage,
        sanitized: SanitizedArticle,
    ): List<CaptureWarning> {
        val warnings = validated.warnings.toMutableList()
        if (sanitized.missingAssetKeys.isNotEmpty()) {
            warnings.add(
                CaptureWarning(
                    code = "ASSET_MISSING",
                    message = "Removed ${sanitized.missingAssetKeys.size} image reference(s) with no captured asset.",
                ),
            )
        }
        return warnings
    }

    private fun validateIdempotencyKey(value: String) {
        if (value.length !in 8..128 || !IDEMPOTENCY_KEY_PATTERN.matches(value)) {
            throw InvalidIdempotencyKeyException(
                "An idempotency key of 8-128 characters using letters, digits, '.', '_', ':' or '-' is required.",
            )
        }
    }

    private fun digest(vararg parts: ByteArray?): String {
        val md = MessageDigest.getInstance("SHA-256")
        parts.filterNotNull().forEach { md.update(it) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun CaptureJob.toResponse(): CaptureResponse {
        val warnings = warnings
            ?.let { objectMapper.readValue(it, Array<CaptureWarningDto>::class.java).toList() }
            ?: emptyList()
        return CaptureResponse(
            captureId = publicId,
            status = status.name,
            contentItemId = contentItemId,
            artifactId = artifactVersionId,
            artifactType = artifactType?.name,
            sourceUrl = sourceUrl,
            warnings = warnings,
            error = errorCode?.let { CaptureErrorDto(it, errorMessage ?: "The capture failed.") },
        )
    }

    companion object {
        private val IDEMPOTENCY_KEY_PATTERN = Regex("^[A-Za-z0-9._:-]+$")
    }
}
