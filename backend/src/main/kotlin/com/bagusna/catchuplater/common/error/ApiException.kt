package com.bagusna.catchuplater.common.error

import org.springframework.http.HttpStatus

/**
 * Base class for expected, client-visible application errors. Each error has an
 * explicit HTTP status and a stable machine-readable code.
 */
open class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
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
