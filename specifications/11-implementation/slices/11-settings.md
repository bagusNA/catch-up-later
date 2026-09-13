# Slice 11 — Settings & System Info

**Milestone:** `S11 Settings`
**Outcome:** Users can manage their account, reader defaults, extension
connection, and inspect storage/system state.
**Depends on:** `S2`–`S8` (account parts depend on `S1`).

## Goal

Finish the settings surface and the destructive-action dialogs.

## In scope

- Account settings: profile, password change, logout.
- Reader defaults: font size, content width, theme, line height.
- Extension connection/setup guidance and reconnection.
- Storage/system info: usage, artifact count, version, backup status/guidance.
- Destructive-action dialogs: delete item, delete artifact/version, remove tag,
  logout, clear failed capture.

## Out of scope

- Admin user management (future).
- Highlight/annotation preferences (deferred).

## Acceptance criteria

- [ ] Account changes persist and are reflected in the shell.
- [ ] Reader defaults apply to newly opened content and can be overridden
      per-session.
- [ ] The settings page explains how to install and connect the extension and
      shows current connection status.
- [ ] Storage info shows artifact usage and count and links to backup guidance.
- [ ] Every destructive action requires explicit confirmation and reports
      outcome via toast.
- [ ] Settings are keyboard accessible and responsive.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S11.1 | Build account settings (profile, password, logout) | frontend | feature |
| S11.2 | Build reader defaults settings and persistence | frontend | feature |
| S11.3 | Build extension connection/setup guidance with status | frontend | feature |
| S11.4 | Build storage and system information view | frontend | feature |
| S11.5 | Build destructive-action confirmation dialogs and toasts | frontend | feature |
| S11.6 | Add settings persistence and dialog tests | frontend | test |

## Verification

- Change reader defaults, open an item, confirm they apply.
- Exercise each destructive dialog and confirm the documented outcome.

## Risks

- Settings can silently diverge from reader-session overrides; document and test
  the precedence (session override > account default).
