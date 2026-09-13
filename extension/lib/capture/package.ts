import { strToU8, zipSync } from 'fflate'
import { SCHEMA_VERSION } from './constants'
import type { AdapterCaptureResult, CaptureManifest, CaptureManifestAsset, CaptureManifestMetadata } from './types'

/**
 * Builds a schema v1 capture package ZIP.
 *
 * Layout:
 * ```text
 * manifest.json
 * content.html
 * content.txt
 * assets/{assetKey}
 * ```
 */
export function buildCapturePackage(result: AdapterCaptureResult): Uint8Array {
  const manifest: CaptureManifest = {
    schemaVersion: SCHEMA_VERSION,
    source: result.source,
    artifact: {
      type: result.artifact.type,
      title: result.artifact.title,
      language: result.artifact.language,
      readingTimeMinutes: result.artifact.readingTimeMinutes,
    },
    metadata: toManifestMetadata(result.metadata),
    assets: result.assets.map(toManifestAsset),
    capture: {
      capturedAt: new Date().toISOString(),
      warnings: result.warnings,
    },
  }

  const files: Record<string, Uint8Array> = {
    'manifest.json': strToU8(JSON.stringify(manifest)),
    'content.html': strToU8(result.artifact.html),
    'content.txt': strToU8(result.artifact.text),
  }
  for (const asset of result.assets) {
    files[`assets/${asset.assetKey}`] = asset.bytes
  }

  return zipSync(files, { level: 6 })
}

function toManifestMetadata(metadata: AdapterCaptureResult['metadata']): CaptureManifestMetadata {
  return {
    author: metadata.author,
    description: metadata.description,
    siteName: metadata.siteName,
    publishedAt: metadata.publishedAt,
    image: metadata.imageAssetKey ? { assetKey: metadata.imageAssetKey } : undefined,
  }
}

function toManifestAsset(asset: AdapterCaptureResult['assets'][number]): CaptureManifestAsset {
  return {
    assetKey: asset.assetKey,
    mimeType: asset.mimeType,
    originalUrl: asset.originalUrl,
    altText: asset.altText,
    byteSize: asset.bytes.byteLength,
    checksum: asset.checksum,
    width: asset.width,
    height: asset.height,
  }
}

/** Base64-encodes bytes in chunks so large packages do not overflow the stack. */
export function toBase64(bytes: Uint8Array): string {
  const chunkSize = 0x8000
  let binary = ''
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize))
  }
  return btoa(binary)
}

export function fromBase64(value: string): Uint8Array {
  const binary = atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}
