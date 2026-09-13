# Decision Log

Resolved ambiguities from the baseline specification. These decisions are
normative for the implementation phase and supersede the baseline where they
conflict.

## DEC-001 — Remove Inbox; processing lives in Library

**Context:** The baseline lists an Inbox route and primary nav item but never
defines its behavior.

**Decision:** Remove Inbox entirely. Items that are `PROCESSING` or `FAILED`
appear inline at the top of the Library with status affordances (progress,
retry, cancel, error). There is no separate inbox.

**Consequences:** Remove `/inbox` from `05-frontend/01-frontend-architecture.md`
and Inbox from the primary navigation in `05-frontend/03-ui-guidelines.md`.
Delete the demo `frontend/app/pages/inbox.vue`.

## DEC-002 — Versioned API and error envelope

**Context:** The existing backend uses `/api/auth/...` and the error shape
`{ timestamp, status, code, message, path, fieldErrors }`. The spec defines
`/api/v1/...` and `{ error: { code, message, details, requestId } }`.

**Decision:** Adopt the spec. All endpoints move under `/api/v1`. All errors use
the spec envelope. Existing auth endpoints are migrated in `S1`; clients are
updated in the same slice.

**Consequences:** Update backend controllers, security handlers, and the
frontend/extension API clients. Error codes are stable public identifiers.

## DEC-003 — Bearer tokens for the extension

**Context:** The backend authenticates the web UI with session cookies + CSRF.
Cross-origin cookie handling from a browser extension is fragile.

**Decision:** The extension authenticates with short-lived **access tokens** and
a **refresh token**, issued by `/api/v1/auth/token` and
`/api/v1/auth/token/refresh`, revoked on logout. The web UI keeps session
cookie + CSRF authentication. Tokens are stored with extension-secure storage
and are never injected into page content.

**Consequences:** Backend gains a token issuance/rotation path; both auth
mechanisms must resolve to the same user and ownership model.

## DEC-004 — ZIP capture package over multipart upload

**Context:** The package contract allows multipart, ZIP, or JSON + binary parts.

**Decision:** The capture package is a **ZIP archive** containing
`manifest.json` plus content and assets, streamed as a single multipart upload.
Assets are referenced by package-local keys, never filesystem paths.

**Consequences:** Backend validates the archive bounds and streams entries to
staging. Large PDFs are handled without loading the whole archive into memory.

## DEC-005 — First-run bootstrap instead of open registration

**Context:** The app is private and self-hosted, but the existing backend leaves
self-registration enabled.

**Decision:** Open registration is disabled by default. The first account is
created through a first-run setup flow (only available while no user exists).
Subsequent users require an admin action or a disabled-by-default invite. The
MVP primary experience remains single-user.

**Consequences:** Add a setup/bootstrap endpoint and UI; ownership stays on all
records for future multi-user.

## DEC-006 — Two distinct status fields

**Context:** `ContentItem.status` and `ReadingState.status` both exist.

**Decision:** `ContentItem.status` is the **capture/processing** status
(`QUEUED`, `UPLOADING`, `PROCESSING`, `READY`, `FAILED`, `CANCELLED`).
`ReadingState.status` is the **reading** status (`UNREAD`, `IN_PROGRESS`,
`READ`). They are never conflated.

**Consequences:** The Library filters on reading status; processing status is a
separate visual affordance.

## DEC-007 — PDF text extraction in the MVP

**Context:** The spec requires full-text search "over extracted PDF text where
available" but does not say where extraction happens.

**Decision:** Extract text server-side with a maintained JVM library (Apache
PDFBox) after PDF validation. Extraction failure is non-fatal: the PDF remains
readable and the capture reports a warning. Search covers extracted text when
present.

**Consequences:** PDF text extraction is part of `S7`; the capture status
exposes `pdfTextExtracted`.

## DEC-008 — Defer design-doc features beyond MVP scope

**Context:** `05-frontend/03-ui-guidelines.md` and `04-design-system.md` mention
home statistics, random "might be interested" content, section/quote bookmarks,
highlights, notes, and a share action. None are in the MVP scope; share is an
explicit non-goal.

**Decision:** Defer all of the above to the deferred backlog. The MVP ships
Library, Favorites, Tags, Settings, article reader, and PDF reader only.

**Consequences:** No highlight/annotation data model in the MVP. The reader
selection tooltip is not implemented.

## DEC-009 — Frontend starts from the Nuxt UI dashboard shell

**Context:** `frontend/` is an unmodified Nuxt UI Dashboard template.

**Decision:** Keep the application shell (layout, sidebar, theme switch,
command palette) but delete the demo pages, components, and mock server routes.
Replace the `green/zinc` theme with the **Editorial Reading Room** tokens.

**Consequences:** `S0` strips the template and installs design tokens; `S1`
builds the real navigation.

## DEC-010 — Normalized design tokens

**Context:** The design system front-matter and prose disagree on the primary
color (`#882c18` / `#a13e29` vs `#A8432D`).

**Decision:** Canonical tokens for implementation:

| Token | Value |
|-------|-------|
| `primary` | `#A8432D` |
| `primary-hover` | `#8F2D1F` |
| `secondary` | `#4A6B6C` |
| `tertiary` | `#C47D38` |
| `surface` | `#F9F7F2` |
| `surface-container` | `#F0EDE4` |
| `outline` | `#E3DFD5` |
| `on-surface` | `#23211E` |
| `on-surface-variant` | `#57534E` |
| `error` | `#8F2D1F` |

Typography: **Newsreader** for prose, **Hanken Grotesk** for UI,
**JetBrains Mono** for metadata.

**Consequences:** Tokens live in one frontend file and are the only source for
component styling.

## DEC-011 — Vertical slices tracked by milestone

**Context:** Progress must be easy to see in GitHub.

**Decision:** One milestone per slice, one issue per task. Task IDs match the
slice documents. Labels use `area:*`, `type:*`, `priority:*`.

**Consequences:** Milestone completion percentage is the progress signal; no
GitHub Project board is required (the token lacks `project` scope).

## DEC-012 — Trunk-based development with per-task PRs

**Decision:** Work lands on `main` via short-lived, squashed PRs. CI runs on
every PR and push. See `01-delivery-plan.md`.

## DEC-013 — Technical defaults

| Concern | Choice |
|---------|--------|
| HTML sanitization (backend) | OWASP Java HTML Sanitizer (pinned policy), re-run independently of the extension |
| HTML sanitization (extension) | DOMPurify before packaging (defense in depth only) |
| PDF parsing/rendering (backend) | Apache PDFBox |
| Artifact storage | Filesystem behind an `ArtifactStorage` interface |
| Full-text search | SQLite FTS5 |
| API docs | springdoc-openapi under `/api/v1` |

## DEC-014 — The web client is a single-page app

**Context:** Session-cookie authentication plus server-side rendering requires
forwarding cookies between the Nuxt server and the backend, and makes the first
render depend on the backend being reachable from the server.

**Decision:** The web client renders as an SPA (`ssr: false`). All API calls go
through the browser over the same-origin `/api` path (dev proxy in development,
reverse proxy in production).

**Consequences:** No SSR cookie forwarding and one consistent auth path. The
library is private, so server-side rendering adds no value.

## DEC-015 — Opaque, hashed, rotating bearer tokens

**Context:** DEC-003 chose bearer tokens for the extension but left the format
open.

**Decision:** Access and refresh tokens are opaque 256-bit random strings stored
only as SHA-256 hashes in SQLite (`auth_tokens`). Access tokens live 15 minutes;
refresh tokens live 30 days and rotate on every use. Tokens belong to a family;
replaying a rotated refresh token revokes the whole family. Changing the
password revokes every token for the account.

**Consequences:** Tokens are revocable with no signing-key management, and a
leaked database does not yield usable credentials. Each bearer request performs
one indexed lookup.

## DEC-016 — Capture package v1 layout and reserved asset host

**Context:** DEC-004 chose a ZIP package but left the concrete entry layout and
how HTML references local assets undefined.

**Decision:** A schema v1 package is a ZIP with `manifest.json`, `content.html`,
`content.txt`, and `assets/{assetKey}`. Article images are rewritten to the
reserved, never-resolvable host `https://assets.cul.invalid/{assetKey}`. The
backend sanitizer preserves only image sources on that host and the reader
rewrites them to `/api/v1/artifacts/{artifactId}/assets/{assetKey}`. The full
schema is documented in `specifications/03-capture/04-capture-package-schema-v1.md`.

**Consequences:** External image URLs are stripped on ingest, so stored
artifacts never depend on the source site. The reserved host is a sentinel, not
a fetch target, which keeps the backend free of SSRF-prone asset fetching.

## DEC-017 — Capture processing is synchronous in the MVP

**Context:** The capture lifecycle allows asynchronous processing, and the API
exposes `GET /captures/{captureId}` for polling.

**Decision:** `POST /api/v1/captures` reads, validates, sanitizes, and stores
the package synchronously and returns the final status. The capture job is still
persisted, and the status endpoint remains, so the contract does not change
when background processing is introduced.

**Consequences:** Failures are recorded as `FAILED` jobs without rolling back
the job row, using `@Transactional(noRollbackFor = [ApiException::class])`.
A future slice may move processing to a worker without touching the API shape.

## DEC-018 — Cross-origin asset download happens in the background worker

**Context:** Article images are frequently served from a CDN on a different
origin than the page. In MV3, content-script `fetch` is subject to the page's
CORS policy, so cross-origin images were dropped and captured articles rendered
without them.

**Decision:** The injected page script only extracts content, assigns
package-local asset keys, and rewrites image `src` values; it never downloads
bytes. The background service worker downloads the referenced images (sending
cookies where needed) and builds the ZIP archive. The extension declares
`<all_urls>` as a **host permission**, granted at install time.

**Consequences:** Cross-origin images are captured without a per-save prompt.
The extension does not browse or read pages at rest — capture is still only
triggered by an explicit user action — but the manifest does ask for broad host
access up front. `fflate` plus the package builder move from the page bundle to
the service worker.

## DEC-019 — FTS5 index lives in a sidecar SQLite database

**Context:** SQLite reports FTS5 virtual-table columns with an empty type name,
which Hibernate's schema extractor cannot parse. With `ddl-auto=validate` the
application fails to start as soon as the virtual table exists in the main
schema, and Hibernate's schema `SchemaFilter` is applied after column
extraction, so it cannot hide the table.

**Decision:** Keep the `content_search` FTS5 virtual table in its own SQLite
database file (`app.search.database-path`, default `./data/search.db`), created
by `SearchDatabaseConfig`. Application code indexes and queries it through a
dedicated `JdbcTemplate`, and the main query joins are replaced by an id list
from the index.

**Consequences:** The main schema stays fully validated by Hibernate. The index
is derived data and can be rebuilt at any time (`POST /api/v1/admin/search/reindex`,
`GET /api/v1/admin/search/integrity`), so the two databases are eventually
consistent rather than transactional. Tag assignment changes trigger a reindex
of the affected item.
