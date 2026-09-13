import { strFromU8, unzipSync } from 'fflate'
import { describe, expect, it } from 'vitest'
import { buildCapturePackage, fromBase64, toBase64 } from './package'
import type { AdapterCaptureResult } from './types'

function result(): AdapterCaptureResult {
  return {
    status: 'SUCCESS',
    source: {
      url: 'https://example.com/a',
      adapterId: 'generic-readability',
      adapterVersion: '1.0.0',
    },
    artifact: {
      type: 'ARTICLE_READER',
      title: 'Example',
      html: '<p>Hello</p>',
      text: 'Hello',
      language: 'en',
      readingTimeMinutes: 1,
    },
    metadata: { author: 'A' },
    assets: [
      {
        assetKey: 'asset-1',
        mimeType: 'image/png',
        originalUrl: 'https://example.com/a.png',
        altText: 'alt',
        bytes: new Uint8Array([1, 2, 3]),
        checksum: 'abc',
      },
    ],
    warnings: [],
  }
}

describe('buildCapturePackage', () => {
  it('produces a schema v1 archive with manifest, content, and assets', () => {
    const archive = buildCapturePackage(result())
    const entries = unzipSync(archive)

    expect(Object.keys(entries).sort()).toEqual(
      ['assets/asset-1', 'content.html', 'content.txt', 'manifest.json'].sort(),
    )
    const manifest = JSON.parse(strFromU8(entries['manifest.json']!))
    expect(manifest.schemaVersion).toBe(1)
    expect(manifest.artifact.type).toBe('ARTICLE_READER')
    expect(manifest.assets[0].assetKey).toBe('asset-1')
    expect(manifest.source.adapterId).toBe('generic-readability')
  })

  it('round-trips base64 encoding', () => {
    const bytes = new Uint8Array([0, 1, 2, 250, 255])
    expect(Array.from(fromBase64(toBase64(bytes)))).toEqual(Array.from(bytes))
  })
})
