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

export type ContentType = 'ARTICLE' | 'PDF'
export type CaptureStatus = 'QUEUED' | 'UPLOADING' | 'PROCESSING' | 'READY' | 'FAILED' | 'CANCELLED'
export type ReadingStatus = 'UNREAD' | 'IN_PROGRESS' | 'READ'
export type LibrarySort = 'saved' | 'title' | 'last_read' | 'relevance'

export interface ContentItemSummary {
  id: number
  title: string
  description: string | null
  sourceUrl: string
  sourceName: string | null
  contentType: ContentType
  status: CaptureStatus
  readingStatus: ReadingStatus
  isFavorite: boolean
  readingTimeMinutes: number | null
  tags: string[]
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
  versionNumber: number
  isCurrent: boolean
  artifactType: 'ARTICLE_READER' | 'PDF_DOCUMENT'
  checksum: string
  byteSize: number
  readingTimeMinutes: number | null
  capturedAt: string
  adapterId: string
  adapterVersion: string
  packageSchemaVersion: number
  validationStatus: 'PENDING' | 'VALID' | 'VALID_WITH_WARNINGS' | 'INVALID'
}

export interface TagResponse {
  id: number
  name: string
  itemCount: number
  createdAt: string
}

export interface ReadingStatePosition {
  type: string | null
  value: number | null
}

export interface ReadingStateResponse {
  contentItemId: number
  status: ReadingStatus
  progressPercent: number | null
  position: ReadingStatePosition | null
  lastReadAt: string | null
  updatedAt: string | null
}

export interface ContentItemDetailResponse {
  contentItem: ContentItemSummary
  readingState: ReadingStateResponse
  artifacts: ArtifactSummary[]
  warnings: CaptureWarning[]
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
