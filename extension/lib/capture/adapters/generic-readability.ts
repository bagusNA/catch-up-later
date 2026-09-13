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
    neutralizeMediaWrapperClasses(clone)
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

/**
 * Readability scores elements by class/id and deletes anything matching its
 * "negative" pattern. Sites commonly wrap legitimate article media in
 * wrappers whose class contains words like `share`, `media`, or `promo` (e.g.
 * Apple's `image-sharesheet`), which removes the image along with the wrapper.
 *
 * Before extraction we strip only those negative class tokens and ids from the
 * ancestors of media elements, so the image survives while Readability's
 * cleanup of unrelated junk is preserved.
 */
const NEGATIVE_CLASS_PATTERN =
  /-ad-|hidden|^hid$| hid$| hid |^hid |banner|combx|comment|com-|contact|footer|gdpr|masthead|media|meta|outbrain|promo|related|scroll|share|shoutbox|sidebar|skyscraper|sponsor|shopping|tags|widget/i

function neutralizeMediaWrapperClasses(root: Document): void {
  const media = root.querySelectorAll('img, picture, figure, video, object, iframe')
  media.forEach((element) => {
    let current: Element | null = element
    while (current && current.tagName !== 'BODY' && current.tagName !== 'HTML') {
      const className = current.getAttribute('class')
      if (className && NEGATIVE_CLASS_PATTERN.test(className)) {
        current.setAttribute(
          'class',
          className.split(/\s+/).filter(token => token && !NEGATIVE_CLASS_PATTERN.test(token)).join(' '),
        )
      }
      const id = current.getAttribute('id')
      if (id && NEGATIVE_CLASS_PATTERN.test(id)) {
        current.removeAttribute('id')
      }
      current = current.parentElement
    }
  })
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
