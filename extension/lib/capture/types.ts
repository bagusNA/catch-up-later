/**
 * Capture package schema v1 types (extension side).
 *
 * These mirror `specifications/03-capture/01-capture-package-contract.md` and
 * the backend's `CaptureManifest`. The schema version is independent of the
 * database schema.
 */

export type ArtifactType = 'ARTICLE_READER' | 'PDF_DOCUMENT'

export type AdapterStatus = 'SUCCESS' | 'SUCCESS_WITH_WARNINGS' | 'FAILURE'

export type CaptureErrorCode =
  | 'UNSUPPORTED_SOURCE'
  | 'EXTRACTION_FAILED'
  | 'INVALID_PACKAGE'
  | 'UPLOAD_FAILED'
  | 'AUTHENTICATION_FAILED'
  | 'STORAGE_FAILED'
  | 'PROCESSING_FAILED'
  | 'ASSET_LIMIT_EXCEEDED'
  | 'CONTENT_TOO_LARGE'
  | 'RATE_LIMITED'
  | 'UNKNOWN'

export interface SourceInfo {
  url: string
  canonicalUrl?: string
  sourceName?: string
  adapterId: string
  adapterVersion: string
}

export interface ArtifactPayload {
  type: ArtifactType
  title: string
  html: string
  text: string
  language?: string
  readingTimeMinutes?: number
}

export interface ExtractedMetadata {
  author?: string
  description?: string
  siteName?: string
  publishedAt?: string
  imageAssetKey?: string
}

export interface CaptureWarning {
  code: string
  message: string
}

export interface CaptureError {
  code: CaptureErrorCode
  message: string
  retryable: boolean
}

/** A binary asset captured from the page, before packaging. */
export interface CapturedAsset {
  assetKey: string
  mimeType: string
  originalUrl: string
  altText?: string
  width?: number
  height?: number
  bytes: Uint8Array
  checksum?: string
}

export interface AdapterCaptureResult {
  status: AdapterStatus
  source: SourceInfo
  artifact: ArtifactPayload
  metadata: ExtractedMetadata
  assets: CapturedAsset[]
  warnings: CaptureWarning[]
  error?: CaptureError
}

export interface CaptureManifestAsset {
  assetKey: string
  mimeType: string
  originalUrl?: string
  altText?: string
  byteSize: number
  checksum?: string
  width?: number
  height?: number
}

export interface CaptureManifestMetadata {
  author?: string
  description?: string
  siteName?: string
  publishedAt?: string
  image?: { assetKey: string }
}

export interface CaptureManifest {
  schemaVersion: 1
  source: SourceInfo
  artifact: {
    type: ArtifactType
    title: string
    language?: string
    readingTimeMinutes?: number
  }
  metadata: CaptureManifestMetadata
  assets: CaptureManifestAsset[]
  capture: {
    capturedAt: string
    warnings: CaptureWarning[]
  }
}
