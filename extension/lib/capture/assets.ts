import {
  ALLOWED_ASSET_MIME_TYPES,
  CAPTURE_LIMITS,
  RESERVED_ASSET_PREFIX,
} from './constants'
import type { CaptureWarning, CapturedAsset } from './types'

export interface AssetCollectionResult {
  /** Article HTML with captured image sources rewritten to asset keys. */
  html: string
  assets: CapturedAsset[]
  /** Asset key of the article's lead image, when one was captured. */
  imageAssetKey?: string
  warnings: CaptureWarning[]
}

export interface CollectAssetsOptions {
  baseUrl: string
  /** Optional preferred lead-image URL, e.g. `og:image`. */
  leadImageUrl?: string
}

/**
 * Collects and downloads the images required to render an article offline,
 * then rewrites the HTML to reference package-local asset keys.
 *
 * Failures are non-fatal: an image that cannot be fetched or validated is
 * removed from the HTML and reported as a warning, so the article still
 * becomes readable.
 */
export async function collectAndRewriteAssets(
  html: string,
  options: CollectAssetsOptions,
): Promise<AssetCollectionResult> {
  const parser = new DOMParser()
  const document = parser.parseFromString(`<body>${html}</body>`, 'text/html')
  const warnings: CaptureWarning[] = []
  const assets: CapturedAsset[] = []
  const keyByUrl = new Map<string, string>()
  let totalBytes = 0
  let limitReached = false
  let leadImageAssetKey: string | undefined

  const images = Array.from(document.querySelectorAll('img'))
  for (const image of images) {
    if (limitReached) {
      removeImageSource(image)
      continue
    }

    const source = image.getAttribute('src') ?? image.getAttribute('data-src')
    if (!source) continue

    const resolved = resolveSource(source, options.baseUrl)
    if (!resolved) {
      removeImageSource(image)
      warnings.push(warning('ASSET_UNRESOLVED', 'Removed an image with an unresolvable URL.'))
      continue
    }

    const existingKey = keyByUrl.get(resolved)
    if (existingKey) {
      rewriteImage(image, existingKey)
      continue
    }

    if (assets.length >= CAPTURE_LIMITS.maxAssetCount) {
      limitReached = true
      removeImageSource(image)
      warnings.push(warning('ASSET_LIMIT_EXCEEDED', 'Stopped capturing images after reaching the asset count limit.'))
      continue
    }

    const fetched = await fetchAsset(resolved)
    if (!fetched) {
      removeImageSource(image)
      warnings.push(warning('ASSET_MISSING', 'Removed an image that could not be captured.'))
      continue
    }

    if (fetched.bytes.byteLength > CAPTURE_LIMITS.maxAssetBytes) {
      removeImageSource(image)
      warnings.push(warning('ASSET_TOO_LARGE', 'Removed an image that exceeds the per-asset size limit.'))
      continue
    }

    if (totalBytes + fetched.bytes.byteLength > CAPTURE_LIMITS.maxTotalAssetBytes) {
      limitReached = true
      removeImageSource(image)
      warnings.push(warning('ASSET_LIMIT_EXCEEDED', 'Stopped capturing images after reaching the total size limit.'))
      continue
    }

    const assetKey = `asset-${assets.length + 1}`
    keyByUrl.set(resolved, assetKey)
    totalBytes += fetched.bytes.byteLength
    assets.push({
      assetKey,
      mimeType: fetched.mimeType,
      originalUrl: resolved,
      altText: image.getAttribute('alt') ?? undefined,
      width: numericAttribute(image, 'width'),
      height: numericAttribute(image, 'height'),
      bytes: fetched.bytes,
      checksum: await sha256Hex(fetched.bytes),
    })
    rewriteImage(image, assetKey)

    if (options.leadImageUrl && sameUrl(resolved, options.leadImageUrl, options.baseUrl)) {
      leadImageAssetKey = assetKey
    }
  }

  return {
    html: document.body.innerHTML,
    assets,
    imageAssetKey: leadImageAssetKey,
    warnings,
  }
}

interface FetchedAsset {
  mimeType: string
  bytes: Uint8Array
}

async function fetchAsset(url: string): Promise<FetchedAsset | null> {
  if (url.startsWith('data:')) {
    return decodeDataUrl(url)
  }
  try {
    const response = await fetch(url, {
      credentials: 'include',
      referrerPolicy: 'no-referrer',
    })
    if (!response.ok) return null
    const bytes = new Uint8Array(await response.arrayBuffer())
    if (bytes.byteLength === 0) return null
    const mimeType = sniffImageMime(bytes)
    if (!mimeType || !ALLOWED_ASSET_MIME_TYPES.has(mimeType)) return null
    return { mimeType, bytes }
  } catch {
    return null
  }
}

function decodeDataUrl(url: string): FetchedAsset | null {
  const match = /^data:(image\/[a-z0-9.+-]+);base64,(.*)$/is.exec(url)
  if (!match) return null
  try {
    const binary = atob(match[2] ?? '')
    const bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index)
    }
    const mimeType = sniffImageMime(bytes)
    if (!mimeType || !ALLOWED_ASSET_MIME_TYPES.has(mimeType)) return null
    return { mimeType, bytes }
  } catch {
    return null
  }
}

/** Detects the image MIME type from magic bytes, or null when unsupported. */
export function sniffImageMime(bytes: Uint8Array): string | null {
  if (startsWith(bytes, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])) return 'image/png'
  if (startsWith(bytes, [0xff, 0xd8, 0xff])) return 'image/jpeg'
  if (startsWith(bytes, [0x47, 0x49, 0x46, 0x38])) return 'image/gif'
  if (
    bytes.length >= 12 &&
    ascii(bytes, 0, 4) === 'RIFF' &&
    ascii(bytes, 8, 4) === 'WEBP'
  ) {
    return 'image/webp'
  }
  return null
}

function rewriteImage(image: Element, assetKey: string): void {
  image.setAttribute('src', `${RESERVED_ASSET_PREFIX}${assetKey}`)
  image.removeAttribute('srcset')
  image.removeAttribute('data-src')
  image.removeAttribute('loading')
}

function removeImageSource(image: Element): void {
  image.removeAttribute('src')
  image.removeAttribute('srcset')
  image.removeAttribute('data-src')
}

function resolveSource(source: string, baseUrl: string): string | null {
  if (source.startsWith('data:')) return source
  try {
    const url = new URL(source, baseUrl)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return null
    return url.toString()
  } catch {
    return null
  }
}

function sameUrl(left: string, right: string, baseUrl: string): boolean {
  try {
    return new URL(left, baseUrl).toString() === new URL(right, baseUrl).toString()
  } catch {
    return false
  }
}

function numericAttribute(element: Element, name: string): number | undefined {
  const raw = element.getAttribute(name)
  if (!raw) return undefined
  const value = Number.parseInt(raw, 10)
  return Number.isFinite(value) && value > 0 ? value : undefined
}

export async function sha256Hex(bytes: Uint8Array): Promise<string | undefined> {
  if (!globalThis.crypto?.subtle) {
    return undefined
  }
  const buffer = await crypto.subtle.digest('SHA-256', bytes as unknown as BufferSource)
  return Array.from(new Uint8Array(buffer))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

function startsWith(bytes: Uint8Array, magic: number[]): boolean {
  if (bytes.length < magic.length) return false
  return magic.every((value, index) => bytes[index] === value)
}

function ascii(bytes: Uint8Array, offset: number, length: number): string {
  return Array.from(bytes.slice(offset, offset + length))
    .map(byte => String.fromCharCode(byte))
    .join('')
}

function warning(code: string, message: string): CaptureWarning {
  return { code, message }
}
