import type { ContentType, LibrarySort, ReadingStatus } from '~/types/api'

export const LIBRARY_PAGE_SIZES = [12, 24, 48] as const

export interface LibraryFilters {
  q: string
  contentType: ContentType | ''
  status: ReadingStatus | ''
  favorite: boolean
  tag: string
  sort: LibrarySort
  page: number
  pageSize: number
}

export const DEFAULT_LIBRARY_FILTERS: LibraryFilters = {
  q: '',
  contentType: '',
  status: '',
  favorite: false,
  tag: '',
  sort: 'saved',
  page: 0,
  pageSize: 12
}

export const READING_STATUS_LABELS: Record<ReadingStatus, string> = {
  UNREAD: 'Unread',
  IN_PROGRESS: 'In progress',
  READ: 'Read'
}

export const CAPTURE_STATUS_LABELS: Record<string, string> = {
  QUEUED: 'Queued',
  UPLOADING: 'Uploading',
  PROCESSING: 'Processing',
  READY: 'Ready',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled'
}

export const SORT_OPTIONS: Array<{ value: LibrarySort, label: string }> = [
  { value: 'saved', label: 'Recently saved' },
  { value: 'title', label: 'Title (A–Z)' },
  { value: 'last_read', label: 'Last read' },
  { value: 'relevance', label: 'Relevance' }
]

function first(value: unknown): string | undefined {
  if (Array.isArray(value)) return value[0]
  return typeof value === 'string' ? value : undefined
}

/** Parses the URL query into normalized library filters. */
export function parseLibraryFilters(query: Record<string, unknown>): LibraryFilters {
  const contentType = first(query.contentType)
  const status = first(query.status)
  const sort = first(query.sort)
  const page = Number.parseInt(first(query.page) ?? '0', 10)
  const pageSize = Number.parseInt(first(query.pageSize) ?? String(DEFAULT_LIBRARY_FILTERS.pageSize), 10)

  return {
    q: first(query.q) ?? '',
    contentType: contentType === 'ARTICLE' || contentType === 'PDF' ? contentType : '',
    status: status === 'UNREAD' || status === 'IN_PROGRESS' || status === 'READ' ? status : '',
    favorite: first(query.favorite) === 'true',
    tag: first(query.tag) ?? '',
    sort: isLibrarySort(sort) ? sort : DEFAULT_LIBRARY_FILTERS.sort,
    page: Number.isNaN(page) || page < 0 ? 0 : page,
    pageSize: Number.isNaN(pageSize) || pageSize < 1 ? DEFAULT_LIBRARY_FILTERS.pageSize : pageSize
  }
}

/** Serializes filters into a trimmed query object for the API client and URL. */
export function buildLibraryQuery(filters: Partial<LibraryFilters>): Record<string, string | number | boolean> {
  const query: Record<string, string | number | boolean> = {}
  const merged = { ...DEFAULT_LIBRARY_FILTERS, ...filters }
  if (merged.q.trim()) query.q = merged.q.trim()
  if (merged.contentType) query.contentType = merged.contentType
  if (merged.status) query.status = merged.status
  if (merged.favorite) query.favorite = true
  if (merged.tag) query.tag = merged.tag
  if (merged.sort && merged.sort !== 'saved') query.sort = merged.sort
  if (merged.page > 0) query.page = merged.page
  query.pageSize = merged.pageSize
  return query
}

/** Builds the router query object (strings only) for a filter state. */
export function buildRouteQuery(filters: Partial<LibraryFilters>): Record<string, string> {
  const query = buildLibraryQuery(filters)
  return Object.fromEntries(Object.entries(query).map(([key, value]) => [key, String(value)]))
}

export function isLibrarySort(value: unknown): value is LibrarySort {
  return value === 'saved' || value === 'title' || value === 'last_read' || value === 'relevance'
}

export function hasActiveFilters(filters: LibraryFilters): boolean {
  return Boolean(filters.q || filters.contentType || filters.status || filters.favorite || filters.tag)
}
