# Development Runbook

How to run Catch Up Later locally. For the delivery plan and conventions see
[`specifications/11-implementation/`](../specifications/11-implementation/README.md).

## Repository layout

```text
backend/     Spring Boot + Kotlin API, SQLite, Flyway migrations
frontend/    Nuxt + Vue + Nuxt UI web client
extension/   WXT + Vue browser extension
specifications/  Product, domain, and implementation specifications
docs/        Operational documentation (this runbook)
```

## Prerequisites

- JDK 25 (Gradle uses a Java toolchain)
- Node.js 22+ and pnpm
- Docker (optional, for the Compose flow)

## Backend

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

- Runs on `http://localhost:8080`.
- The SQLite database lives at `backend/data/app.db` (created on first run).
- The `local` profile disables the `Secure` session-cookie flag so plain HTTP
  works. Never use it in production.
- Flyway applies migrations at startup; Hibernate runs in `validate` mode.

Health check:

```bash
curl http://localhost:8080/api/v1/health
# {"status":"UP","application":"catch-up-later"}
```

Tests:

```bash
cd backend
./gradlew test
```

Tests run against real, file-based SQLite databases under
`backend/build/test-data/`. Each test context also gets an isolated artifact
storage directory.

### Capture storage

Captured packages are validated, re-sanitized, and stored on the filesystem:

- Immutable artifacts: `backend/data/artifacts/{ownerId}/{contentItemId}/{version}/`
- In-progress uploads: `backend/data/staging/{stagingId}/`

Files are written to staging and moved into place atomically before the
database is marked ready. Size and count limits live under `app.capture` in
`backend/src/main/resources/application.yaml`; the package format is documented
in [`specifications/03-capture/04-capture-package-schema-v1.md`](../specifications/03-capture/04-capture-package-schema-v1.md).

### Backend via Docker Compose

```bash
docker compose up --build
```

The backend is built from `backend/Dockerfile` and stores its database in the
named volume `backend-data`. Remove volumes with `docker compose down -v`.

## Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

- Runs on `http://localhost:3000` as a single-page app (`ssr: false`, DEC-014).
- On a fresh backend, `/setup` creates the first account; afterwards `/login`
  signs in.
- Requests to `/api/**` are proxied to the backend origin
  (`NUXT_API_PROXY_TARGET`, default `http://localhost:8080`).
- The API client uses `runtimeConfig.public.apiBase` (`/api/v1` by default) and
  adds the `X-XSRF-TOKEN` header to state-changing requests.
- Copy `.env.example` to `.env` to override either value.

Checks:

```bash
pnpm lint
pnpm typecheck
```

## Extension

```bash
cd extension
pnpm install
pnpm dev          # Chrome, loads an unpacked build
pnpm dev:firefox
pnpm compile      # type-check
pnpm test         # adapter and packaging fixture tests
```

The extension only captures on explicit user action. It requests `activeTab`,
`scripting`, and `tabs` rather than broad host permissions at rest. On the
first save it asks for the optional `<all_urls>` host permission so the
background worker can download cross-origin article images; if you decline,
same-origin images still work and the rest become warnings. Open the
extension's options page, enter the backend URL and your account credentials,
and connect. Tokens are stored with `browser.storage.local` and refreshed by the
background worker; the popup never talks to the backend directly. On a supported
article, click **Save this page** in the popup; the page context extracts and
rewrites image references, then the service worker downloads assets, builds the
package, and uploads it.

## Environment variables

| App | Variable | Default | Purpose |
|-----|----------|---------|---------|
| frontend | `NUXT_API_PROXY_TARGET` | `http://localhost:8080` | Backend origin for the dev proxy |
| frontend | `NUXT_PUBLIC_API_BASE` | `/api/v1` | API base path used by the client |
| backend | `APP_*` | see `application.yaml` | Application settings |

## Conventions

- All public JSON endpoints live under `/api/v1` and use the error envelope
  `{ "error": { "code", "message", "details", "requestId" } }`.
- The web client authenticates with session cookies and CSRF; the extension uses
  bearer access/refresh tokens (DEC-003, DEC-015).
- The frontend reaches the backend through the single `useApi()` client; do not
  call `$fetch` directly from components.
- The extension talks to the backend from its background service worker only.
- One branch per GitHub issue, named `slice-<n>-<short-name>`; PRs reference the
  slice document and close the issue.

## Definition of done

See [`specifications/11-implementation/01-delivery-plan.md`](../specifications/11-implementation/01-delivery-plan.md#definition-of-done-every-slice).
