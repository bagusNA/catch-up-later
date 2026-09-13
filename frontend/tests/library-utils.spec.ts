import { describe, expect, it } from 'vitest'
import {
  buildLibraryQuery,
  buildRouteQuery,
  hasActiveFilters,
  parseLibraryFilters
} from '~/utils/library'

describe('parseLibraryFilters', () => {
  it('falls back to defaults for missing or invalid values', () => {
    const filters = parseLibraryFilters({})
    expect(filters).toEqual({
      q: '',
      contentType: '',
      status: '',
      favorite: false,
      tag: '',
      sort: 'saved',
      page: 0,
      pageSize: 12
    })

    const invalid = parseLibraryFilters({
      contentType: 'VIDEO',
      status: 'PAUSED',
      sort: 'unknown',
      page: '-3',
      pageSize: 'nope'
    })
    expect(invalid.contentType).toBe('')
    expect(invalid.status).toBe('')
    expect(invalid.sort).toBe('saved')
    expect(invalid.page).toBe(0)
    expect(invalid.pageSize).toBe(12)
  })

  it('normalizes valid filters', () => {
    const filters = parseLibraryFilters({
      q: '  orbital  ',
      contentType: 'PDF',
      status: 'IN_PROGRESS',
      favorite: 'true',
      tag: 'space',
      sort: 'last_read',
      page: '2',
      pageSize: '24'
    })
    expect(filters).toMatchObject({
      q: '  orbital  ',
      contentType: 'PDF',
      status: 'IN_PROGRESS',
      favorite: true,
      tag: 'space',
      sort: 'last_read',
      page: 2,
      pageSize: 24
    })
    expect(hasActiveFilters(filters)).toBe(true)
  })
})

describe('buildLibraryQuery', () => {
  it('omits empty filters and default paging', () => {
    expect(buildLibraryQuery({})).toEqual({ pageSize: 12 })
  })

  it('trims the query and includes active filters', () => {
    const query = buildLibraryQuery({
      q: '  rocket  ',
      contentType: 'ARTICLE',
      status: 'UNREAD',
      favorite: true,
      tag: 'science',
      sort: 'title',
      page: 1,
      pageSize: 24
    })
    expect(query).toEqual({
      q: 'rocket',
      contentType: 'ARTICLE',
      status: 'UNREAD',
      favorite: true,
      tag: 'science',
      sort: 'title',
      page: 1,
      pageSize: 24
    })
  })

  it('serializes for the router as strings', () => {
    expect(buildRouteQuery({ favorite: true, page: 2 })).toEqual({
      favorite: 'true',
      page: '2',
      pageSize: '12'
    })
  })
})
