import { WORDS_PER_MINUTE } from './constants'

/** Estimates reading time from extracted plain text. */
export function readingTimeMinutes(text: string): number {
  const words = text.trim().split(/\s+/).filter(Boolean).length
  return Math.max(1, Math.ceil(words / WORDS_PER_MINUTE))
}
