import { Readability } from '@mozilla/readability'
import { collectAndRewriteAssets } from '../assets'
import { sanitizeArticleHtml } from '../sanitize'
import { readingTimeMinutes } from '../reading-time'
import type { AdapterCaptureResult, CaptureWarning, ExtractedMetadata } from '../types'
import type { CaptureContext, SourceAdapter } from './source-adapter'

/**
 * Generic article adapter backed by Mozilla Readability.
 *
 * It handles ordinary article pages and is the lowest-priority fallback. It
 * extracts normalized HTML and text, captures referenced images, and returns a
 * categorized failure when Readability cannot find an article.
 */
export class GenericReadabilityAdapter implements SourceAdapter {
  readonly id = 'generic-readability'
  readonly version = '1.0.0'
  readonly priority = 0

  canHandle(context: CaptureContext): boolean {
    if (context.document.contentType === 'application/pdf') {
      return false
    }
    if (!context.url.startsWith('http://') && !context.url.startsWith('https://')) {
      return false
    }
    return context.document.body != null
  }

  async capture(context: CaptureContext): Promise<AdapterCaptureResult> {
    const document = context.document
    const title = document.title?.trim() || context.url
    const source = {
      url: context.url,
      canonicalUrl: canonicalLink(document) ?? undefined,
      sourceName: metaContent(document, 'og:site_name') ?? undefined,
      adapterId: this.id,
      adapterVersion: this.version,
    }

    const clone = document.cloneNode(true) as Document
    let parsed: ReturnType<Readability['parse']>
    try {
      parsed = new Readability(clone, { charThreshold: 100 }).parse()
    } catch {
      return this.failure(source, 'EXTRACTION_FAILED', 'The page could not be parsed as an article.')
    }

    if (!parsed?.content || !parsed.textContent?.trim()) {
      return this.failure(source, 'EXTRACTION_FAILED', 'Readability could not find article content on this page.')
    }

    const collected = await collectAndRewriteAssets(parsed.content, {
      baseUrl: context.url,
      leadImageUrl: metaContent(document, 'og:image') ?? undefined,
    })
    const html = sanitizeArticleHtml(collected.html)
    const text = parsed.textContent.trim()
    const warnings: CaptureWarning[] = [...collected.warnings]

    const siteName = nonBlank(parsed.siteName) ?? source.sourceName
    source.sourceName = siteName ?? undefined

    const metadata: ExtractedMetadata = {
      author: nonBlank(parsed.byline) ?? metaContent(document, 'article:author'),
      description: nonBlank(parsed.excerpt) ?? metaContent(document, 'description'),
      siteName: siteName ?? undefined,
      publishedAt: toIsoDate(nonBlank(parsed.publishedTime) ?? metaContent(document, 'article:published_time')),
      imageAssetKey: collected.imageAssetKey,
    }

    return {
      status: warnings.length > 0 ? 'SUCCESS_WITH_WARNINGS' : 'SUCCESS',
      source,
      artifact: {
        type: 'ARTICLE_READER',
        title: nonBlank(parsed.title) ?? title,
        html,
        text,
        language: nonBlank(parsed.lang) ?? document.documentElement.lang ?? undefined,
        readingTimeMinutes: readingTimeMinutes(text),
      },
      metadata,
      assets: collected.assets,
      warnings,
    }
  }

  private failure(
    source: AdapterCaptureResult['source'],
    code: 'EXTRACTION_FAILED' | 'UNSUPPORTED_SOURCE',
    message: string,
  ): AdapterCaptureResult {
    return {
      status: 'FAILURE',
      source,
      artifact: { type: 'ARTICLE_READER', title: '', html: '', text: '' },
      metadata: {},
      assets: [],
      warnings: [],
      error: { code, message, retryable: false },
    }
  }
}

function canonicalLink(document: Document): string | null {
  const href = document.querySelector('link[rel="canonical"]')?.getAttribute('href')
  if (!href) return null
  try {
    return new URL(href, document.baseURI).toString()
  } catch {
    return null
  }
}

function metaContent(document: Document, selector: string): string | undefined {
  const value = document.querySelector(`meta[property="${selector}"], meta[name="${selector}"]`)
    ?.getAttribute('content')
  return nonBlank(value)
}

function nonBlank(value: string | null | undefined): string | undefined {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

/** Normalizes a source publication date to ISO-8601, or drops it if invalid. */
function toIsoDate(value: string | undefined): string | undefined {
  if (!value) return undefined
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString()
}
