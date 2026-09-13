import { buildCapturePackage, toBase64 } from './package'
import { defaultAdapters } from './adapters'
import { selectAdapter, type SourceAdapter } from './adapters/source-adapter'
import type {
  ArtifactType,
  CaptureError,
  CaptureWarning,
  SourceInfo,
} from './types'

export interface CapturePipelineSuccess {
  ok: true
  packageBase64: string
  source: SourceInfo
  warnings: CaptureWarning[]
  artifactType: ArtifactType
  title: string
}

export interface CapturePipelineFailure {
  ok: false
  error: CaptureError
}

export type CapturePipelineResult = CapturePipelineSuccess | CapturePipelineFailure

/**
 * Runs adapter selection, extraction, sanitization, and packaging against a
 * document. This is the single entry point executed inside the page context.
 */
export async function capturePage(
  document: Document = globalThis.document,
  adapters: readonly SourceAdapter[] = defaultAdapters(),
): Promise<CapturePipelineResult> {
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

  const bytes = buildCapturePackage(result)
  return {
    ok: true,
    packageBase64: toBase64(bytes),
    source: result.source,
    warnings: result.warnings,
    artifactType: result.artifact.type,
    title: result.artifact.title,
  }
}

function failure(code: CaptureError['code'], message: string): CapturePipelineFailure {
  return { ok: false, error: error(code, message) }
}

function error(code: CaptureError['code'], message: string): CaptureError {
  return { code, message, retryable: false }
}
