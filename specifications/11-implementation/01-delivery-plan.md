# Delivery Plan

## Objective

Deliver the Catch Up Later MVP as a sequence of vertical slices. Each slice
provides a demonstrable, user-visible capability and leaves `main` in a
releasable state.

## Slicing rules

1. **Vertical, not layered.** A slice may touch the extension, backend, and
   frontend, but it must produce one observable outcome.
2. **Thin but complete.** Prefer the smallest path that works end-to-end over a
   broad partial implementation. Polish lands in later slices.
3. **Always shippable.** Every merge keeps CI green and the product runnable.
4. **Fix the contract once.** Capture package, API, and reader payload contracts
   are versioned and changed deliberately, never as a side effect.
5. **Security is continuous.** Sanitization, ownership checks, and limits are
   implemented in the slice that introduces the surface, then verified again in
   `S9`.
6. **Progress is visible.** Each task maps to a GitHub issue under the slice
   milestone.

## Dependency graph

```text
S0 Foundation
  └── S1 Identity
        └── S2 Save generic article → read
              ├── S3 Library & discovery
              ├── S4 Capture resilience
              ├── S5 Wikipedia adapter
              ├── S6 Medium adapter
              └── S7 PDF end-to-end
                    └── S8 Reader experience
                          └── S9 Security hardening
                                └── S10 Operations
                                      └── S11 Settings & system info
```

`S3`–`S7` are independent of each other once `S2` lands and may be reordered.
`S8` benefits from `S7` for PDF progress. `S9` revisits every surface.

## Definition of Done (every slice)

A slice is complete when:

- [ ] All slice acceptance criteria are met and demonstrated.
- [ ] Tasks are merged to `main` through reviewed PRs.
- [ ] CI is green: backend tests, frontend lint + typecheck, extension compile.
- [ ] Automated tests cover the new behavior at the appropriate level
      (unit / integration / e2e).
- [ ] Migrations are added forward-only and validated by a startup test.
- [ ] No secrets are committed; new configuration is documented.
- [ ] Spec/decision docs are updated when behavior changes.
- [ ] The slice milestone is fully closed.
- [ ] A short demo note (what to click / curl) is added to the PR description.

## Branching and PRs

- Trunk-based development on `main`.
- One branch per task: `slice-<n>-<short-name>`; one PR per task.
- PRs reference their issue and the relevant slice file.
- Squash-merge; delete the branch after merge.
- A slice branch may be used to stage several tasks when they are tightly
  coupled, but each task still has its own issue.
- Rebase on `main` before merging.

## CI

GitHub Actions, one workflow per app, triggered on pull requests and pushes to
`main`:

| App | Command |
|-----|---------|
| backend | `./gradlew test` |
| frontend | `pnpm install && pnpm lint && pnpm typecheck` |
| extension | `pnpm install && pnpm compile` |

A combined workflow may aggregate the three. `S0` establishes these workflows.

## Environments

- **Local**: backend on HTTP with the `local` profile, SQLite under
  `backend/data/`, frontend on `:3000`, extension in WXT dev mode.
- **Self-hosted production**: Docker Compose, HTTPS terminated at a reverse
  proxy, artifacts and database on persistent volumes.
- **Test**: in-process or file-based SQLite per test run, never the dev database.

## Technical conventions

- Backend package root: `com.bagusna.catchuplater`, organized by feature under
  `features/<feature>` with `api`, `application`, `domain`, and `persistence`
  sub-packages.
- All JSON APIs live under `/api/v1`.
- Error envelope: `{ "error": { "code", "message", "details", "requestId" } }`.
- Frontend API access goes through one typed client; components never call
  `$fetch` directly.
- Extension talks to the backend through the background service worker only.
- Capture package schema is versioned independently of the database.

## Deferred backlog

Explicitly **not** in the MVP slice plan. Each item may become its own milestone
after `S11`:

- Home dashboard with reading statistics.
- "Continue reading" and "might be interested" (random) sections.
- Bookmarks of a specific section or quote.
- Highlights, notes, and a selection tooltip.
- Sharing (permanent non-goal).
- Wikipedia refresh as an explicit source capability.
- Export/import archive.
- Additional adapters: Substack, documentation sites, academic repositories.
- Additional artifact types: video/audio reference, transcript, EPUB, web archive.
- Mobile-native application and browser-history ingestion.

## Risks

| Risk | Mitigation |
|------|------------|
| Readability quality varies by site | Honest failure/warning states; adapter fallback in later slices |
| Extension cross-origin auth complexity | Short-lived bearer tokens + refresh, decided in `S1` |
| Large PDFs exhausting memory | Streaming ZIP upload, size limits enforced early |
| Sanitization gaps | Backend re-sanitizes independently; dedicated hardening slice |
| Scope creep from design docs | Deferred backlog is explicit; MVP scope is frozen |
| SQLite write concurrency | WAL, busy timeout, single-writer application services |
