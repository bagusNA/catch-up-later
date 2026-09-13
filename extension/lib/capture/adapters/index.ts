import { GenericReadabilityAdapter } from './generic-readability'
import type { SourceAdapter } from './source-adapter'

/**
 * The adapter registry. Adapters are ordered by priority during selection, so
 * the order here is only a default. `S5`-`S7` add specialized adapters with
 * higher priorities.
 */
export function defaultAdapters(): SourceAdapter[] {
  return [new GenericReadabilityAdapter()]
}

export { GenericReadabilityAdapter } from './generic-readability'
export { selectAdapter } from './source-adapter'
export type { CaptureContext, SourceAdapter } from './source-adapter'
