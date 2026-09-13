package com.bagusna.catchuplater.common.error

import org.springframework.http.HttpStatus

/**
 * Base class for expected, client-visible application errors. Each error has an
 * explicit HTTP status, a stable machine-readable code, and optional structured
 * details.
 */
open class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)

class DuplicateEmailException : ApiException(
    status = HttpStatus.CONFLICT,
    code = "DUPLICATE_EMAIL",
    message = "An account with this email already exists.",
)

class WeakPasswordException(message: String) : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "WEAK_PASSWORD",
    message = message,
)

class RegistrationDisabledException : ApiException(
    status = HttpStatus.FORBIDDEN,
    code = "REGISTRATION_DISABLED",
    message = "Account registration is currently disabled.",
)

class SetupAlreadyCompletedException : ApiException(
    status = HttpStatus.CONFLICT,
    code = "SETUP_ALREADY_COMPLETED",
    message = "Initial setup has already been completed.",
)

class InvalidTokenException : ApiException(
    status = HttpStatus.UNAUTHORIZED,
    code = "INVALID_TOKEN",
    message = "The token is invalid or has expired.",
)

class InvalidPasswordException : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "INVALID_PASSWORD",
    message = "The current password is incorrect.",
)

class ResourceNotFoundException(message: String = "The requested resource was not found.") : ApiException(
    status = HttpStatus.NOT_FOUND,
    code = "NOT_FOUND",
    message = message,
)

/**
 * The uploaded capture package is missing required entries, is malformed, or
 * fails schema validation.
 */
class InvalidCapturePackageException(
    message: String,
    details: Map<String, Any?> = emptyMap(),
) : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "CAPTURE_INVALID_PACKAGE",
    message = message,
    details = details,
)

/** The artifact type is not supported by this backend version. */
class UnsupportedArtifactTypeException(message: String) : ApiException(
    status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
    code = "CAPTURE_UNSUPPORTED_ARTIFACT_TYPE",
    message = message,
)

/** The package or one of its entries exceeds a configured limit. */
class CaptureTooLargeException(message: String) : ApiException(
    status = HttpStatus.PAYLOAD_TOO_LARGE,
    code = "CONTENT_TOO_LARGE",
    message = message,
)

/** Too many assets, or the asset payload exceeds the total budget. */
class AssetLimitExceededException(message: String) : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "ASSET_LIMIT_EXCEEDED",
    message = message,
)

/** The idempotency key is missing or not in the accepted format. */
class InvalidIdempotencyKeyException(message: String) : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "INVALID_IDEMPOTENCY_KEY",
    message = message,
)

class CaptureNotFoundException : ApiException(
    status = HttpStatus.NOT_FOUND,
    code = "CAPTURE_NOT_FOUND",
    message = "The capture was not found.",
)

class ArtifactUnavailableException(message: String = "The artifact is not available.") : ApiException(
    status = HttpStatus.NOT_FOUND,
    code = "ARTIFACT_UNAVAILABLE",
    message = message,
)

/** A tag with the same case-insensitive name already exists for this owner. */
class DuplicateTagException : ApiException(
    status = HttpStatus.CONFLICT,
    code = "TAG_EXISTS",
    message = "A tag with this name already exists.",
)

/** The supplied tag name is empty or too long. */
class InvalidTagException(message: String) : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "INVALID_TAG",
    message = message,
)

/** The supplied reading status or position payload is invalid. */
class InvalidReadingStateException(message: String) : ApiException(
    status = HttpStatus.BAD_REQUEST,
    code = "INVALID_READING_STATE",
    message = message,
)

/**
 * A capture could not be processed. Unlike most [ApiException]s this does not
 * roll back the capture job's failure state, so the client can poll it.
 */
class CaptureProcessingException(
    message: String,
    details: Map<String, Any?> = emptyMap(),
) : ApiException(
    status = HttpStatus.UNPROCESSABLE_ENTITY,
    code = "PROCESSING_FAILED",
    message = message,
    details = details,
)
