/**
 * Types for the versioned JSON API (see DEC-002).
 */

export interface FieldValidationError {
  field: string
  message: string
}

export interface ApiErrorBody {
  code: string
  message: string
  details: Record<string, unknown>
  requestId: string
}

export interface ApiErrorEnvelope {
  error: ApiErrorBody
}
