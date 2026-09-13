import { afterEach, describe, expect, it, vi } from 'vitest'
import { GenericReadabilityAdapter } from './generic-readability'
import { selectAdapter, type SourceAdapter } from './source-adapter'
import { RESERVED_ASSET_PREFIX } from '../constants'

const PNG = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02, 0x03])

function buildDocument(innerHtml: string, title = 'Sample article'): Document {
  const document = window.document.implementation.createHTMLDocument(title)
  document.body.innerHTML = innerHtml
  return document
}

function articleHtml(imageTag = ''): string {
  const paragraph = 'This is a sufficiently long sentence about reading and capturing content. '.repeat(6)
  return `<article><h1>Sample article</h1><p>${paragraph}</p>${imageTag}</article>`
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('GenericReadabilityAdapter', () => {
  it('recognizes ordinary http(s) article pages', () => {
    const adapter = new GenericReadabilityAdapter()
    expect(adapter.canHandle({ document: buildDocument(articleHtml()), url: 'https://example.com/a' })).toBe(true)
    expect(adapter.canHandle({ document: buildDocument(articleHtml()), url: 'chrome://extensions' })).toBe(false)
  })

  it('extracts title, text, and reading time', async () => {
    const adapter = new GenericReadabilityAdapter()
    const result = await adapter.capture({
      document: buildDocument(articleHtml()),
      url: 'https://example.com/a',
    })

    expect(result.status === 'SUCCESS' || result.status === 'SUCCESS_WITH_WARNINGS').toBe(true)
    expect(result.artifact.title).toBe('Sample article')
    expect(result.artifact.text).toContain('sufficiently long sentence')
    expect(result.artifact.readingTimeMinutes).toBeGreaterThanOrEqual(1)
    expect(result.source.adapterId).toBe('generic-readability')
  })

  it('fails in a categorized way when there is no article content', async () => {
    const adapter = new GenericReadabilityAdapter()
    const result = await adapter.capture({
      document: buildDocument('<p></p>'),
      url: 'https://example.com/empty',
    })

    expect(result.status).toBe('FAILURE')
    expect(result.error?.code).toBe('EXTRACTION_FAILED')
  })

  it('captures images and rewrites them to package-local asset keys', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      arrayBuffer: async () => PNG.buffer.slice(0),
    })))

    const adapter = new GenericReadabilityAdapter()
    const result = await adapter.capture({
      document: buildDocument(articleHtml('<img src="/hero.png" alt="Hero">')),
      url: 'https://example.com/a',
    })

    expect(result.assets).toHaveLength(1)
    expect(result.assets[0]?.mimeType).toBe('image/png')
    expect(result.artifact.html).toContain(`${RESERVED_ASSET_PREFIX}asset-1`)
  })

  it('still succeeds with a warning when assets cannot be captured', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false })))

    const adapter = new GenericReadabilityAdapter()
    const result = await adapter.capture({
      document: buildDocument(articleHtml('<img src="/missing.png" alt="Missing">')),
      url: 'https://example.com/a',
    })

    expect(result.assets).toHaveLength(0)
    expect(result.status).toBe('SUCCESS_WITH_WARNINGS')
    expect(result.warnings.some(warning => warning.code === 'ASSET_MISSING')).toBe(true)
  })
})

describe('selectAdapter', () => {
  it('prefers the highest-priority applicable adapter', async () => {
    const generic: SourceAdapter = {
      id: 'generic', version: '1', priority: 0,
      canHandle: () => true,
      capture: vi.fn(),
    }
    const specialized: SourceAdapter = {
      id: 'specialized', version: '1', priority: 10,
      canHandle: () => true,
      capture: vi.fn(),
    }

    const selected = await selectAdapter([generic, specialized], {
      document: buildDocument(''),
      url: 'https://example.com',
    })
    expect(selected?.id).toBe('specialized')
  })

  it('returns null when no adapter applies', async () => {
    const none: SourceAdapter = {
      id: 'none', version: '1', priority: 0,
      canHandle: () => false,
      capture: vi.fn(),
    }
    expect(await selectAdapter([none], { document: buildDocument(''), url: 'https://example.com' })).toBeNull()
  })
})
