import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchImageAssets } from './image-assets'
import type { ImageReference } from './types'

const PNG_BYTES = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4])
const PNG_DATA_URL =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=='

function reference(url: string, key = 'asset-1'): ImageReference {
  return { assetKey: key, url, altText: 'alt' }
}

function stubFetch(impl: (url: string) => unknown): void {
  vi.stubGlobal('fetch', vi.fn(async (input: unknown) => impl(String(input))))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchImageAssets', () => {
  it('downloads and validates cross-origin images in the worker', async () => {
    stubFetch(() => ({ ok: true, arrayBuffer: async () => PNG_BYTES.buffer.slice(0) }))

    const result = await fetchImageAssets([reference('https://cdn.example.com/hero.png')])

    expect(result.assets).toHaveLength(1)
    expect(result.assets[0]).toMatchObject({
      assetKey: 'asset-1',
      mimeType: 'image/png',
      originalUrl: 'https://cdn.example.com/hero.png',
    })
    expect(result.assets[0]?.checksum).toMatch(/^[0-9a-f]{64}$/)
    expect(result.warnings).toHaveLength(0)
  })

  it('decodes embedded data URLs without a network request', async () => {
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)

    const result = await fetchImageAssets([reference(PNG_DATA_URL)])

    expect(fetchSpy).not.toHaveBeenCalled()
    expect(result.assets).toHaveLength(1)
    expect(result.assets[0]?.mimeType).toBe('image/png')
  })

  it('omits images whose bytes are not a supported image format', async () => {
    stubFetch(() => ({ ok: true, arrayBuffer: async () => new TextEncoder().encode('not an image').buffer }))

    const result = await fetchImageAssets([reference('https://cdn.example.com/fake.png', 'asset-1')])

    expect(result.assets).toHaveLength(0)
    expect(result.warnings[0]?.code).toBe('ASSET_MISSING')
  })

  it('reports a warning when the download fails', async () => {
    stubFetch(() => ({ ok: false }))

    const result = await fetchImageAssets([reference('https://cdn.example.com/missing.jpg')])

    expect(result.assets).toHaveLength(0)
    expect(result.warnings.some(warning => warning.code === 'ASSET_MISSING')).toBe(true)
  })
})
