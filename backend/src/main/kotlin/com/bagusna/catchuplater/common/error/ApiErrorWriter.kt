package com.bagusna.catchuplater.common.error

import tools.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Writes the canonical [ApiError] payload to a raw servlet response. Used from
 * Spring Security filter handlers, which run outside of the MVC exception
 * handling pipeline.
 */
@Component
class ApiErrorWriter(
    private val objectMapper: ObjectMapper,
) {
    fun write(
        response: HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String,
        path: String,
        fieldErrors: List<FieldValidationError> = emptyList(),
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.outputStream,
            ApiError(
                timestamp = Instant.now(),
                status = status.value(),
                code = code,
                message = message,
                path = path,
                fieldErrors = fieldErrors,
            ),
        )
    }
}
