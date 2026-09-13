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

export interface CaptureWarning {
  code: string
  message: string
}

export interface ContentItemSummary {
  id: number
  title: string
  description: string | null
  sourceUrl: string
  sourceName: string | null
  contentType: 'ARTICLE' | 'PDF'
  status: 'QUEUED' | 'UPLOADING' | 'PROCESSING' | 'READY' | 'FAILED' | 'CANCELLED'
  isFavorite: boolean
  readingTimeMinutes: number | null
  createdAt: string
  lastReadAt: string | null
}

export interface ContentItemListResponse {
  items: ContentItemSummary[]
  page: number
  pageSize: number
  total: number
}

export interface ArtifactSummary {
  id: number
  artifactType: 'ARTICLE_READER' | 'PDF_DOCUMENT'
  checksum: string
  byteSize: number
  capturedAt: string
  adapterId: string
  adapterVersion: string
  packageSchemaVersion: number
  validationStatus: 'PENDING' | 'VALID' | 'VALID_WITH_WARNINGS' | 'INVALID'
}

export interface ReaderMetadata {
  author: string | null
  description: string | null
  siteName: string | null
  publishedAt: string | null
  language: string | null
  readingTimeMinutes: number | null
}

export interface ReaderResponse {
  contentItem: ContentItemSummary
  artifact: ArtifactSummary
  metadata: ReaderMetadata
  html: string
  warnings: CaptureWarning[]
}
