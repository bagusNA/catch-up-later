import type { AdapterCaptureResult } from '../types'

/** Everything an adapter may inspect during capture. */
export interface CaptureContext {
  document: Document
  url: string
}

/**
 * Contract every source adapter implements.
 *
 * Adapters are pure with respect to persistence: they read the page, produce
 * an [AdapterCaptureResult], and never touch backend or storage concerns.
 */
export interface SourceAdapter {
  readonly id: string
  readonly version: string
  /** Higher priority adapters are evaluated first. */
  readonly priority: number
  canHandle(context: CaptureContext): boolean | Promise<boolean>
  capture(context: CaptureContext): Promise<AdapterCaptureResult>
}

/**
 * Selects the highest-priority adapter that can handle the context. Returns
 * null when no adapter claims the page.
 */
export async function selectAdapter(
  adapters: readonly SourceAdapter[],
  context: CaptureContext,
): Promise<SourceAdapter | null> {
  const ordered = [...adapters].sort((left, right) => right.priority - left.priority)
  for (const adapter of ordered) {
    if (await adapter.canHandle(context)) {
      return adapter
    }
  }
  return null
}
