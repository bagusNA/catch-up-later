import { browser } from 'wxt/browser'
import { authenticatedFetch } from './auth'
import { fromBase64 } from './capture/package'
import type { CapturePipelineResult } from './capture/pipeline'
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
 * Runs entirely in the background service worker: the page only ever returns
 * an inert package, and tokens never enter page content.
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
  const result = injection[0]?.result as CapturePipelineResult | undefined
  if (!result) {
    throw new Error('The page did not return a capture package.')
  }
  if (!result.ok) {
    return {
      captureId: null,
      status: 'FAILED',
      contentItemId: null,
      warnings: [],
      error: { code: result.error.code, message: result.error.message },
    }
  }

  return uploadPackage(result.packageBase64)
}

async function uploadPackage(packageBase64: string): Promise<CaptureSummary> {
  const bytes = fromBase64(packageBase64)
  const form = new FormData()
  form.append('package', new Blob([bytes as unknown as BlobPart], { type: 'application/zip' }), 'capture.zip')

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
