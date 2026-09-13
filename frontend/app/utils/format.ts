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
