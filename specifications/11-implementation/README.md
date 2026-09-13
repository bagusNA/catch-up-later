# Implementation Layer

This folder turns the baseline specification (folders `01-product` … `10-roadmap`)
into an executable delivery plan.

## How to use this layer

- `01-delivery-plan.md` — slicing strategy, Definition of Done, branching/CI,
  dependency graph, and deferred backlog.
- `02-decisions.md` — resolved ambiguities and technical decisions (ADRs). Where
  this layer and the baseline spec conflict, **this layer wins**; the decision
  log records the delta.
- `slices/` — one file per vertical slice. Each slice file is the source of
  truth for its milestone and issues.

## Vertical slices

Every slice is a thin end-to-end path through extension, backend, and frontend
(or the subset relevant to it). A slice is "done" only when it can be
demonstrated from a user-visible action, not when a layer is finished.

| # | Slice | Milestone | Outcome |
|---|-------|-----------|---------|
| 0 | [Foundation](slices/00-foundation.md) | `S0 Foundation` | CI green for all three apps, dev environment, conventions, ADRs |
| 1 | [Identity](slices/01-identity.md) | `S1 Identity` | Login/logout on web, extension connects and authenticates |
| 2 | [Save generic article → read](slices/02-save-generic-article.md) | `S2 Core capture` | Save any article in the extension, read it in the library |
| 3 | [Library & discovery](slices/03-library-discovery.md) | `S3 Library` | Search, filter, sort, tags, favorites, status, delete |
| 4 | [Capture resilience](slices/04-capture-resilience.md) | `S4 Resilience` | Retry, idempotency, duplicates, honest failure states |
| 5 | [Wikipedia adapter](slices/05-wikipedia-adapter.md) | `S5 Wikipedia` | Wikipedia pages captured with high fidelity |
| 6 | [Medium adapter](slices/06-medium-adapter.md) | `S6 Medium` | Medium articles captured from the browser context |
| 7 | [PDF end-to-end](slices/07-pdf.md) | `S7 PDF` | Save, index, and read PDFs |
| 8 | [Reader experience](slices/08-reader-experience.md) | `S8 Reader` | Resume, progress, typography controls, degraded states |
| 9 | [Security hardening](slices/09-security-hardening.md) | `S9 Security` | Threat-model verification and controls |
| 10 | [Operations](slices/10-operations.md) | `S10 Operations` | Self-host with backups, health, observability |
| 11 | [Settings & system info](slices/11-settings.md) | `S11 Settings` | Account, reader defaults, extension setup, storage info |

Deferred work is listed in `01-delivery-plan.md` under **Deferred backlog**.

## GitHub tracking

- **One milestone per slice.** The milestone progress bar is the primary
  progress view.
- **One issue per task.** Task IDs (`S2.7`) are stable and match the tables in
  the slice files.
- **Labels** use `area:*`, `type:*`, and `priority:*` prefixes.
- Branch names follow `slice-<n>-<short-name>` (e.g. `slice-2-generic-article`).
- Pull requests reference the issues they close (`Closes #12`).
