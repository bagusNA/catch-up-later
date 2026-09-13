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

export interface AuthUser {
  id: number
  email: string
  displayName: string | null
  roles: string[]
}

export interface SetupStatus {
  required: boolean
}
