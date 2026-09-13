# Slice 0 — Foundation

**Milestone:** `S0 Foundation`
**Outcome:** A runnable monorepo with green CI, documented conventions, design
tokens, and an API error contract, ready for feature slices.
**Depends on:** Nothing.

## Goal

Remove template noise, establish the development loop, and make every later
slice land on a working pipeline.

## In scope

- CI workflows for backend, frontend, and extension.
- Local development runbook and environment variables.
- Frontend template cleanup and Editorial Reading Room theme tokens.
- Backend `/api/v1` routing base and the shared error envelope.
- A typed frontend API client skeleton.
- Decision log published.

## Out of scope

- Any real feature (login, capture, reader).
- Production Docker image, backups, observability (`S10`).

## Acceptance criteria

- [ ] Pushing a branch runs backend tests, frontend lint/typecheck, and
      extension compile in CI.
- [ ] `docker compose up` or the documented local commands start backend and
      frontend; the backend serves `GET /api/v1/health` (or equivalent).
- [ ] Demo pages (customers, inbox, teams, home widgets) and mock server routes
      are removed from the frontend.
- [ ] The `green/zinc` theme is replaced by the canonical tokens from DEC-010.
- [ ] Backend errors return `{ "error": { "code", "message", "details", "requestId" } }`.
- [ ] `specifications/11-implementation/` is committed and linked from the
      specification README.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S0.1 | Add GitHub Actions CI for backend, frontend, extension | infra | feature |
| S0.2 | Write local development runbook (`docs/DEVELOPMENT.md`) | docs | docs |
| S0.3 | Add local `compose.yaml` wiring backend + SQLite volume | infra | feature |
| S0.4 | Strip Nuxt UI dashboard demo pages, components, mock server routes | frontend | feature |
| S0.5 | Install Editorial Reading Room design tokens and fonts | frontend | feature |
| S0.6 | Introduce `/api/v1` routing base and spec error envelope in backend | backend | feature |
| S0.7 | Add typed frontend API client skeleton with error mapping | frontend | feature |
| S0.8 | Clean extension starter (remove HelloWorld, set manifest metadata) | extension | feature |
| S0.9 | Publish decision log and update specification README | docs | docs |

## Verification

- CI is green on `main`.
- Starting the stack from a fresh clone follows only the runbook.
- A deliberately failing request returns the documented error envelope.

## Risks

- Error-envelope migration may ripple through existing auth tests; do it before
  `S1` to keep one migration.
