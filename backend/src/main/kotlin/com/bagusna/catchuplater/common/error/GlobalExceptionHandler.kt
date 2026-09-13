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
import java.time.Instant

/**
 * Maps exceptions raised inside the MVC pipeline to the canonical [ApiError]
 * payload. Errors raised inside Spring Security filters are handled separately
 * by the JSON entry point / access denied handler.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(exception.status, exception.code, exception.message ?: "Request failed.", request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val fieldErrors = exception.bindingResult.fieldErrors.map {
            FieldValidationError(it.field, it.defaultMessage ?: "Invalid value.")
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", request, fieldErrors)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(
        exception: HandlerMethodValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val fieldErrors = exception.parameterValidationResults.flatMap { result ->
            val field = result.methodParameter.parameterName ?: "parameter"
            result.resolvableErrors.map { FieldValidationError(field, it.defaultMessage ?: "Invalid value.") }
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", request, fieldErrors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val fieldErrors = exception.constraintViolations.map {
            FieldValidationError(it.propertyPath.toString(), it.message)
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", request, fieldErrors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is missing or malformed.", request)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        exception: AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.", request)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to access this resource.", request)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(
        exception: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found.", request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        exception: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method is not supported for this resource.", request)

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(
        exception: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "The request content type is not supported.", request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        // Log the full detail server-side, but never leak it to the client.
        log.error("Unhandled exception for {} {}", request.method, request.requestURI, exception)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", request)
    }

    private fun error(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        fieldErrors: List<FieldValidationError> = emptyList(),
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(
                timestamp = Instant.now(),
                status = status.value(),
                code = code,
                message = message,
                path = request.requestURI,
                fieldErrors = fieldErrors,
            ),
        )
}
