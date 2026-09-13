/** Human-readable date used across the library and reader. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  }).format(date)
}

export function formatReadingTime(minutes: number | null | undefined): string {
  if (!minutes || minutes < 1) return ''
  return minutes === 1 ? '1 min read' : `${minutes} min read`
}

export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null || bytes < 0) return ''
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB']
  let value = bytes / 1024
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value.toFixed(value >= 10 ? 0 : 1)} ${units[unit]}`
}
