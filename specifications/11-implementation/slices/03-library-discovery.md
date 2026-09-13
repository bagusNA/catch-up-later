# Slice 3 — Library & Discovery

**Milestone:** `S3 Library`
**Outcome:** The library becomes usable at scale: search, filter, sort, tags,
favorites, reading status, item detail, and deletion.
**Depends on:** `S2`.

## Goal

Turn the minimal list into a manageable personal library and introduce
full-text search and reading state.

## In scope

- FTS5 index lifecycle and reindex operation.
- List API with query, filters, sort, and pagination.
- Reading state (status/progress/position) API.
- Tags CRUD and assignment.
- Favorites.
- Item detail with artifact/version inspection.
- Deletion with artifact cleanup.
- Frontend Library toolbar, filters, views, favorites, tags, search results,
  item detail, empty/loading/error states.

## Out of scope

- Reader resume and typography controls (`S8`).
- Export/import (deferred).

## Acceptance criteria

- [ ] Search matches title, description, source, article text, PDF text, and
      tags, always scoped to the owner.
- [ ] Filters (type, reading status, favorite, tag) and sort (saved, title,
      last read) combine correctly with pagination.
- [ ] Reading status transitions to `IN_PROGRESS` after meaningful progress and
      can be set to `READ` or reverted to `UNREAD`.
- [ ] Tags can be created, assigned, and removed; names are unique per owner
      case-insensitively.
- [ ] Favorites toggle and are filterable.
- [ ] Deleting an item removes or garbage-collects its artifacts and index rows.
- [ ] Item detail shows capture warnings and artifact version metadata.
- [ ] Loading, empty, error, and no-results states are implemented.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S3.1 | Add FTS5 virtual tables and indexing on capture ready | backend | feature |
| S3.2 | Add reindex CLI/endpoint (all, one item, integrity report) | backend | feature |
| S3.3 | Implement list API with q/contentType/status/favorite/tag/sort/page | backend | feature |
| S3.4 | Implement reading-state GET/PUT and status transitions | backend | feature |
| S3.5 | Implement tags CRUD and content-item tag assignment | backend | feature |
| S3.6 | Implement favorite add/remove endpoints | backend | feature |
| S3.7 | Implement delete endpoint with artifact cleanup | backend | feature |
| S3.8 | Implement item detail API and artifact version listing | backend | feature |
| S3.9 | Build LibraryToolbar, filters, sort, and list/grid views | frontend | feature |
| S3.10 | Build pagination or infinite loading | frontend | feature |
| S3.11 | Build Favorites and Tags views | frontend | feature |
| S3.12 | Build content item detail and delete confirmation | frontend | feature |
| S3.13 | Build search results with count and no-results state | frontend | feature |
| S3.14 | Add backend tests for search, filters, ownership, and cleanup | backend | test |
| S3.15 | Add frontend tests for filtering and empty/error states | frontend | test |

## Verification

- Seed ~50 items across types/statuses and exercise every filter combination.
- Confirm a second user cannot see or search the first user's items.
- Delete an item and confirm files and FTS rows are gone.

## Risks

- FTS5 query building is easy to get wrong; keep the index owner-scoped and test
  injection-like input.
- Pagination consistency while items are processing.
