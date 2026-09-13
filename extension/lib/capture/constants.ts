/**
 * Capture limits shared with the backend defaults. The backend re-enforces its
 * own copy; these only prevent the extension from building a package that will
 * certainly be rejected.
 */
export const CAPTURE_LIMITS = {
  maxAssetCount: 100,
  maxAssetBytes: 10 * 1024 * 1024,
  maxTotalAssetBytes: 50 * 1024 * 1024,
  maxHtmlBytes: 5 * 1024 * 1024,
  maxTextBytes: 5 * 1024 * 1024,
} as const

/**
 * Captured images are rewritten to this reserved, never-resolvable host. The
 * backend sanitizer only keeps image sources on this host and the reader
 * rewrites them to ownership-checked asset endpoints.
 */
export const RESERVED_ASSET_HOST = 'assets.cul.invalid'
export const RESERVED_ASSET_PREFIX = `https://${RESERVED_ASSET_HOST}/`

export const SCHEMA_VERSION = 1 as const

export const WORDS_PER_MINUTE = 220

export const ALLOWED_ASSET_MIME_TYPES = new Set([
  'image/png',
  'image/jpeg',
  'image/gif',
  'image/webp',
])
