import { CAPTURE_LIMITS } from './constants'
import { sha256Hex } from './hashing'
import { sniffImageMime } from './image-signature'
import type { CapturedAsset, CaptureWarning, ImageReference } from './types'

export interface FetchAssetsResult {
  assets: CapturedAsset[]
  warnings: CaptureWarning[]
}

/**
 * Downloads the referenced article images in the extension's background
 * service worker.
 *
 * Unlike the page context, the worker can fetch cross-origin URLs because the
 * extension declares the `<all_urls>` host permission, so CDN-hosted images
 * are captured too. Missing or invalid images are non-fatal: they are omitted
 * and reported as warnings, and the backend strips the dangling references.
 */
export async function fetchImageAssets(references: ImageReference[]): Promise<FetchAssetsResult> {
  const assets: CapturedAsset[] = []
  const warnings: CaptureWarning[] = []
  let totalBytes = 0

  for (const reference of references) {
    if (assets.length >= CAPTURE_LIMITS.maxAssetCount) {
      warnings.push(warning('ASSET_LIMIT_EXCEEDED', 'Stopped capturing images after reaching the asset count limit.'))
      break
    }

    const fetched = await fetchImage(reference.url)
    if (!fetched) {
      warnings.push(warning('ASSET_MISSING', 'Removed an image that could not be captured.'))
      continue
    }
    if (fetched.bytes.byteLength > CAPTURE_LIMITS.maxAssetBytes) {
      warnings.push(warning('ASSET_TOO_LARGE', 'Removed an image that exceeds the per-asset size limit.'))
      continue
    }
    if (totalBytes + fetched.bytes.byteLength > CAPTURE_LIMITS.maxTotalAssetBytes) {
      warnings.push(warning('ASSET_LIMIT_EXCEEDED', 'Stopped capturing images after reaching the total size limit.'))
      break
    }

    totalBytes += fetched.bytes.byteLength
    assets.push({
      assetKey: reference.assetKey,
      mimeType: fetched.mimeType,
      originalUrl: reference.url,
      altText: reference.altText,
      width: reference.width,
      height: reference.height,
      bytes: fetched.bytes,
      checksum: await sha256Hex(fetched.bytes),
    })
  }

  return { assets, warnings }
}

interface FetchedImage {
  mimeType: string
  bytes: Uint8Array
}

async function fetchImage(url: string): Promise<FetchedImage | null> {
  if (url.startsWith('data:')) {
    return decodeDataUrl(url)
  }
  try {
    const response = await fetch(url, {
      // Host permissions let the worker read these bytes; credentials are sent
      // so images behind the user's existing session also resolve.
      credentials: 'include',
      cache: 'force-cache',
    })
    if (!response.ok) return null
    const bytes = new Uint8Array(await response.arrayBuffer())
    if (bytes.byteLength === 0) return null
    const mimeType = sniffImageMime(bytes)
    if (!mimeType) return null
    return { mimeType, bytes }
  } catch {
    return null
  }
}

function decodeDataUrl(url: string): FetchedImage | null {
  const match = /^data:(image\/[a-z0-9.+-]+);base64,(.*)$/is.exec(url)
  if (!match) return null
  try {
    const binary = atob(match[2] ?? '')
    const bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index)
    }
    const mimeType = sniffImageMime(bytes)
    if (!mimeType) return null
    return { mimeType, bytes }
  } catch {
    return null
  }
}

function warning(code: string, message: string): CaptureWarning {
  return { code, message }
}
