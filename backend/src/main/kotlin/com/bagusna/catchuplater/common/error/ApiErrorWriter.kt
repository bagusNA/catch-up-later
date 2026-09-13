package com.bagusna.catchuplater.common.error

import com.bagusna.catchuplater.core.web.RequestIdFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Writes the canonical [ApiError] envelope to a raw servlet response. Used from
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
        details: Map<String, Any?> = emptyMap(),
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.outputStream,
            ApiError(
                error = ApiErrorBody(
                    code = code,
                    message = message,
                    details = details,
                    requestId = RequestIdFilter.currentId(),
                ),
            ),
        )
    }
}
