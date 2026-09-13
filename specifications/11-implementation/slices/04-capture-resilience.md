# Slice 4 — Capture Resilience

**Milestone:** `S4 Resilience`
**Outcome:** Captures survive outages and retries without duplicates, with
honest, actionable failure states.
**Depends on:** `S2`.

## Goal

Make capture trustworthy: reliable retries, idempotency, duplicate detection,
cancellation, and clear status in the UI.

## In scope

- Extension persisted, bounded retry queue with exponential backoff.
- Idempotency keys end-to-end.
- Backend retry and cancel endpoints.
- Duplicate detection (URL + checksum) with an explicit response.
- Failure taxonomy surfaced in the UI with retry where retryable.
- Library processing/failed rows with retry and cancel actions.

## Out of scope

- Server-side crawling or refresh (deferred / non-goal).

## Acceptance criteria

- [ ] A capture attempted during a backend outage is queued and retried with
      backoff; it succeeds when the backend returns.
- [ ] Retrying with the same idempotency key returns the original result and
      never creates a second artifact or content item.
- [ ] Re-saving identical content reports a duplicate and does not overwrite.
- [ ] Failures are categorized (`UPLOAD_FAILED`, `PROCESSING_FAILED`,
      `UNSUPPORTED_SOURCE`, …) and retry is offered only when retryable.
- [ ] Processing captures can be cancelled before `READY`.
- [ ] The retry queue is bounded and reports its storage usage.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S4.1 | Implement persisted bounded retry queue with backoff | extension | feature |
| S4.2 | Generate and reuse idempotency keys per capture attempt | extension | feature |
| S4.3 | Add idempotency records and same-result semantics | backend | feature |
| S4.4 | Add `POST /api/v1/captures/{id}/retry` | backend | feature |
| S4.5 | Add `DELETE /api/v1/captures/{id}` cancellation | backend | feature |
| S4.6 | Implement URL + checksum duplicate detection and response | backend | feature |
| S4.7 | Add failure taxonomy to API responses | backend | feature |
| S4.8 | Surface categorized failures and retry in the extension | extension | feature |
| S4.9 | Show processing/failed rows with retry and cancel in Library | frontend | feature |
| S4.10 | Add duplicate-content notice UI | frontend | feature |
| S4.11 | Add retry/idempotency/duplicate integration tests | backend | test |

## Verification

- Kill the backend mid-upload, restart, confirm exactly one artifact.
- Replay the same package twice, confirm one content item.
- Force a permanent validation failure and confirm retry requires a new package.

## Risks

- Duplicate policy edge cases (same URL changed content vs same content new URL).
  Store both signals and document behavior.
- Retry storms after long outages; cap attempts and surface manual retry.
