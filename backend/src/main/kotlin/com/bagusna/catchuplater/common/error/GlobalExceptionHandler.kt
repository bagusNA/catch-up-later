package com.bagusna.catchuplater.common.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Maps exceptions raised inside the MVC pipeline to the canonical [ApiError]
 * envelope. Errors raised inside Spring Security filters are handled separately
 * by the JSON entry point / access denied handler.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException): ResponseEntity<ApiError> =
        error(exception.status, exception.code, exception.message ?: "Request failed.")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(exception: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = exception.bindingResult.fieldErrors.map {
            FieldValidationError(it.field, it.defaultMessage ?: "Invalid value.")
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", fieldErrors)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(exception: HandlerMethodValidationException): ResponseEntity<ApiError> {
        val fieldErrors = exception.parameterValidationResults.flatMap { result ->
            val field = result.methodParameter.parameterName ?: "parameter"
            result.resolvableErrors.map { FieldValidationError(field, it.defaultMessage ?: "Invalid value.") }
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", fieldErrors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<ApiError> {
        val fieldErrors = exception.constraintViolations.map {
            FieldValidationError(it.propertyPath.toString(), it.message)
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", fieldErrors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException): ResponseEntity<ApiError> =
        error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is missing or malformed.")

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(exception: AuthenticationException): ResponseEntity<ApiError> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): ResponseEntity<ApiError> =
        error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to access this resource.")

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(exception: NoResourceFoundException): ResponseEntity<ApiError> =
        error(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found.")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ApiError> =
        error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method is not supported for this resource.")

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(exception: HttpMediaTypeNotSupportedException): ResponseEntity<ApiError> =
        error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "The request content type is not supported.")

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        // Log the full detail server-side, but never leak it to the client.
        log.error("Unhandled exception for {} {}", request.method, request.requestURI, exception)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.")
    }

    private fun error(
        status: HttpStatus,
        code: String,
        message: String,
        fieldErrors: List<FieldValidationError> = emptyList(),
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(
                error = ApiErrorBody(
                    code = code,
                    message = message,
                    details = if (fieldErrors.isEmpty()) emptyMap() else mapOf("fieldErrors" to fieldErrors),
                    requestId = com.bagusna.catchuplater.core.web.RequestIdFilter.currentId(),
                ),
            ),
        )
}
