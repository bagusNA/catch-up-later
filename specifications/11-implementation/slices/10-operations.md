# Slice 10 — Operations

**Milestone:** `S10 Operations`
**Outcome:** The app can be self-hosted, upgraded, backed up, and observed.
**Depends on:** `S2`–`S8` (can start once the core capture pipeline is stable).

## Goal

Deliver the deployment, health, logging, and backup story described in
`08-operations/01-deployment-backup-and-observability.md`.

## In scope

- Production Dockerfile and Docker Compose example with persistent volumes.
- Liveness/readiness endpoints with database and storage checks.
- Structured logging with request, user, capture, and artifact IDs.
- Configuration documented and environment-driven.
- Manual backup, restore, and integrity verification.
- Versioned migrations and upgrade documentation.
- Reindex CLI (shared with `S3`).

## Out of scope

- Managed cloud deployment.
- Metrics backend and dashboards (architecture seam only).

## Acceptance criteria

- [ ] `docker compose up` starts the app against fresh empty volumes and applies
      migrations safely.
- [ ] `/api/v1/health/live` and `/api/v1/health/ready` reflect database and
      artifact storage state.
- [ ] Logs are structured and never contain secrets or full content.
- [ ] A documented backup captures both SQLite and artifacts; restore reproduces
      the library; integrity verification detects a mismatch.
- [ ] The app warns when database and artifact backup timestamps differ.
- [ ] Upgrade docs require a backup before migration.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S10.1 | Add production Dockerfile with multi-stage build | infra | feature |
| S10.2 | Add Docker Compose example with volumes for db and artifacts | infra | feature |
| S10.3 | Add liveness/readiness endpoints with dependency checks | backend | feature |
| S10.4 | Add structured logging with correlation IDs and redaction | backend | feature |
| S10.5 | Document all configuration and environment variables | docs | docs |
| S10.6 | Implement manual backup with integrity verification | backend | feature |
| S10.7 | Implement restore command / documented restore procedure | backend | feature |
| S10.8 | Add reindex and maintenance CLI | backend | feature |
| S10.9 | Write upgrade and migration documentation | docs | docs |
| S10.10 | Add a backup/restore integration test | backend | test |

## Verification

- Deploy from scratch on a clean host, ingest, back up, restore to a new host.
- Corrupt one artifact and confirm integrity verification fails loudly.

## Risks

- Backups are only valid with artifacts; enforce the combined-backup rule and
  verify it in tests.
- Compose file drift from the real image; build in CI.
