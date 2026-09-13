import { browser } from 'wxt/browser'
import { authenticatedFetch } from './auth'
import { fetchImageAssets } from './capture/image-assets'
import { buildCapturePackage } from './capture/package'
import type { CapturePageResult } from './capture/pipeline'
import type { CaptureWarning } from './capture/types'

export interface CaptureSummary {
  captureId: string | null
  status: 'READY' | 'FAILED'
  contentItemId: number | null
  warnings: CaptureWarning[]
  error?: { code: string, message: string }
}

interface CaptureApiResponse {
  captureId: string
  status: string
  contentItemId: number | null
  warnings?: CaptureWarning[]
  error?: { code: string, message: string }
}

const POLL_ATTEMPTS = 15
const POLL_INTERVAL_MS = 1_000

/**
 * Captures the active tab and uploads the resulting package.
 *
 * The page script only extracts and rewrites; this worker downloads the asset
 * bytes (using host permissions so cross-origin images work), builds the
 * archive, and uploads it. Tokens never enter page content.
 */
export async function captureActiveTab(): Promise<CaptureSummary> {
  const [tab] = await browser.tabs.query({ active: true, currentWindow: true })
  if (!tab?.id) {
    throw new Error('No active tab is available to capture.')
  }

  const injection = await browser.scripting.executeScript({
    target: { tabId: tab.id },
    files: ['/capture-extract.js'],
  })
  const extracted = injection[0]?.result as CapturePageResult | undefined
  if (!extracted) {
    throw new Error('The page did not return a capture result.')
  }
  if (!extracted.ok) {
    return {
      captureId: null,
      status: 'FAILED',
      contentItemId: null,
      warnings: [],
      error: { code: extracted.error.code, message: extracted.error.message },
    }
  }

  const { page } = extracted
  const { assets, warnings } = await fetchImageAssets(page.images)
  const archive = buildCapturePackage({
    source: page.source,
    artifact: page.artifact,
    metadata: page.metadata,
    assets,
    warnings: [...page.warnings, ...warnings],
  })

  return uploadPackage(archive)
}

async function uploadPackage(archive: Uint8Array): Promise<CaptureSummary> {
  const form = new FormData()
  form.append('package', new Blob([archive as unknown as BlobPart], { type: 'application/zip' }), 'capture.zip')

  const response = await authenticatedFetch('/api/v1/captures', {
    method: 'POST',
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body: form,
  })

  if (!response.ok) {
    const body = await safeJson(response)
    return {
      captureId: null,
      status: 'FAILED',
      contentItemId: null,
      warnings: [],
      error: body?.error ?? { code: classifyStatus(response.status), message: `Upload failed (${response.status}).` },
    }
  }

  const created = await response.json() as CaptureApiResponse
  let current = created
  for (let attempt = 0; attempt < POLL_ATTEMPTS; attempt += 1) {
    if (current.status === 'READY' || current.status === 'FAILED') {
      break
    }
    await delay(POLL_INTERVAL_MS)
    const polled = await authenticatedFetch(`/api/v1/captures/${current.captureId}`)
    if (!polled.ok) {
      break
    }
    current = await polled.json() as CaptureApiResponse
  }

  if (current.status === 'READY') {
    return {
      captureId: current.captureId,
      status: 'READY',
      contentItemId: current.contentItemId,
      warnings: current.warnings ?? [],
    }
  }

  return {
    captureId: current.captureId,
    status: 'FAILED',
    contentItemId: current.contentItemId ?? null,
    warnings: current.warnings ?? [],
    error: current.error ?? { code: 'PROCESSING_FAILED', message: 'The capture did not finish processing.' },
  }
}

async function safeJson(response: Response): Promise<{ error?: { code: string, message: string } } | null> {
  try {
    return await response.json()
  } catch {
    return null
  }
}

function classifyStatus(status: number): string {
  if (status === 401) return 'AUTHENTICATION_FAILED'
  if (status === 413) return 'CONTENT_TOO_LARGE'
  if (status === 429) return 'RATE_LIMITED'
  return 'UPLOAD_FAILED'
}

function delay(milliseconds: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, milliseconds))
}
