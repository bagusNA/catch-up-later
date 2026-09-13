import type { ApiErrorBody, ApiErrorEnvelope } from '~/types/api'

/**
 * Normalized error thrown by the API client. The backend always returns the
 * `{ error: { code, message, details, requestId } }` envelope, so callers can
 * branch on a stable `code` instead of parsing messages.
 */
export class ApiRequestError extends Error {
  readonly code: string
  readonly status: number | undefined
  readonly details: Record<string, unknown>
  readonly requestId: string | undefined

  constructor(body: Partial<ApiErrorBody>, status?: number) {
    super(body.message ?? 'The request failed.')
    this.name = 'ApiRequestError'
    this.code = body.code ?? 'UNKNOWN'
    this.status = status
    this.details = body.details ?? {}
    this.requestId = body.requestId
  }
}

export function isApiErrorEnvelope(value: unknown): value is ApiErrorEnvelope {
  return (
    typeof value === 'object'
    && value !== null
    && 'error' in value
    && typeof (value as ApiErrorEnvelope).error?.code === 'string'
  )
}

export function toApiRequestError(error: unknown, status?: number): ApiRequestError {
  const data = (error as { data?: unknown } | null)?.data
  if (isApiErrorEnvelope(data)) {
    return new ApiRequestError(data.error, status)
  }
  return new ApiRequestError({}, status)
}
