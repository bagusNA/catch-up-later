import { CAPTURE_LIMITS, RESERVED_ASSET_PREFIX } from './constants'
import type { CaptureWarning, ImageReference } from './types'

export interface ImageCollectionResult {
  /** Article HTML with image sources rewritten to package-local asset keys. */
  html: string
  references: ImageReference[]
  /** Asset key of the article's lead image, when one was referenced. */
  leadImageAssetKey?: string
  warnings: CaptureWarning[]
}

export interface CollectImagesOptions {
  baseUrl: string
  /** Optional preferred lead-image URL, e.g. `og:image`. */
  leadImageUrl?: string
}

/**
 * Finds article images, assigns package-local keys, and rewrites the HTML to
 * the reserved asset host.
 *
 * No bytes are downloaded here: this runs in the page context, where
 * cross-origin `fetch` is blocked by CORS. The background worker downloads the
 * referenced URLs with host permissions instead.
 */
export function collectImageReferences(
  html: string,
  options: CollectImagesOptions,
): ImageCollectionResult {
  const parser = new DOMParser()
  const document = parser.parseFromString(`<body>${html}</body>`, 'text/html')
  const warnings: CaptureWarning[] = []
  const references: ImageReference[] = []
  const keyByUrl = new Map<string, string>()
  let limitReached = false
  let leadImageAssetKey: string | undefined

  for (const image of Array.from(document.querySelectorAll('img'))) {
    if (limitReached) {
      removeImageSource(image)
      continue
    }

    const source = pickSource(image)
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

    if (references.length >= CAPTURE_LIMITS.maxAssetCount) {
      limitReached = true
      removeImageSource(image)
      warnings.push(warning('ASSET_LIMIT_EXCEEDED', 'Stopped referencing images after reaching the asset count limit.'))
      continue
    }

    const assetKey = `asset-${references.length + 1}`
    keyByUrl.set(resolved, assetKey)
    references.push({
      assetKey,
      url: resolved,
      altText: image.getAttribute('alt') ?? undefined,
      width: numericAttribute(image, 'width'),
      height: numericAttribute(image, 'height'),
    })
    rewriteImage(image, assetKey)

    if (options.leadImageUrl && sameUrl(resolved, options.leadImageUrl, options.baseUrl)) {
      leadImageAssetKey = assetKey
    }
  }

  return {
    html: document.body.innerHTML,
    references,
    leadImageAssetKey,
    warnings,
  }
}

/**
 * Prefers explicit lazy-loading attributes over `src`, which is often a
 * low-resolution placeholder on modern sites.
 */
function pickSource(image: Element): string | null {
  for (const attribute of ['data-src', 'data-original', 'data-lazy-src', 'src']) {
    const value = image.getAttribute(attribute)
    if (value) return value
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
  image.removeAttribute('data-original')
  image.removeAttribute('data-lazy-src')
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

function warning(code: string, message: string): CaptureWarning {
  return { code, message }
}
