import { defaultAdapters } from './adapters'
import { selectAdapter, type SourceAdapter } from './adapters/source-adapter'
import type { AdapterCaptureResult, CaptureError } from './types'

export interface CapturePageSuccess {
  ok: true
  page: AdapterCaptureResult
}

export interface CapturePageFailure {
  ok: false
  error: CaptureError
}

export type CapturePageResult = CapturePageSuccess | CapturePageFailure

/**
 * Runs adapter selection, extraction, sanitization, and asset rewriting in the
 * page context.
 *
 * This stops short of downloading asset bytes or building the archive: the
 * background worker does that with host permissions, which is not available to
 * page scripts. Everything returned here is JSON-serializable.
 */
export async function extractPage(
  document: Document = globalThis.document,
  adapters: readonly SourceAdapter[] = defaultAdapters(),
): Promise<CapturePageResult> {
  const url = document.location?.href ?? ''
  if (!url) {
    return failure('UNSUPPORTED_SOURCE', 'The current page has no address.')
  }

  const context = { document, url }
  const adapter = await selectAdapter(adapters, context)
  if (!adapter) {
    return failure('UNSUPPORTED_SOURCE', 'No capture adapter can handle this page.')
  }

  let result
  try {
    result = await adapter.capture(context)
  } catch {
    return failure('EXTRACTION_FAILED', 'The page could not be captured.')
  }

  if (result.status === 'FAILURE') {
    return { ok: false, error: result.error ?? error('EXTRACTION_FAILED', 'The page could not be captured.') }
  }

  return { ok: true, page: result }
}

function failure(code: CaptureError['code'], message: string): CapturePageFailure {
  return { ok: false, error: error(code, message) }
}

function error(code: CaptureError['code'], message: string): CaptureError {
  return { code, message, retryable: false }
}
