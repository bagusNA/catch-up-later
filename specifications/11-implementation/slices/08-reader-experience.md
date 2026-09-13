# Slice 8 — Reader Experience

**Milestone:** `S8 Reader`
**Outcome:** The reader feels like a quiet reading surface: stable resume,
visible progress, typography controls, and honest degraded states.
**Depends on:** `S2` (and `S7` for PDF progress).

## Goal

Deliver the reading experience described in `05-frontend/02-reader-ux.md`.

## In scope

- Stable reading position (artifact ID + percentage + text offset/anchor; PDF page).
- Resume prompt and last-read tracking.
- Font size, content width, and theme controls with persisted defaults.
- Estimated reading time and metadata block.
- Reading progress indicator.
- Degraded/error states: artifact unavailable/invalid, missing asset, no search
  text, no PDF text, source unavailable.
- Keyboard accessibility and reduced-motion support.

## Out of scope

- Highlights, notes, bookmarks, sharing (deferred).

## Acceptance criteria

- [ ] Reopening a partially read article resumes at the saved position.
- [ ] Progress updates consistently across sessions and devices.
- [ ] Font size, content width, and theme changes persist as reader defaults.
- [ ] Reading time is shown for articles and hidden when unavailable.
- [ ] Each degraded state renders a specific, non-blocking message and keeps
      usable content visible.
- [ ] Reader controls are keyboard reachable and progress is announced.
- [ ] `prefers-reduced-motion` is respected.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S8.1 | Define and persist stable reading position model | backend | feature |
| S8.2 | Persist and restore resume position in the article reader | frontend | feature |
| S8.3 | Build reader settings (font, width, theme) with persisted defaults | frontend | feature |
| S8.4 | Build reading progress indicator and last-read tracking | frontend | feature |
| S8.5 | Build reader metadata block and reading time | frontend | feature |
| S8.6 | Implement reader degraded/error states | frontend | feature |
| S8.7 | Add resume-reading prompt | frontend | feature |
| S8.8 | Accessibility pass (keyboard, focus, announcements, reduced motion) | frontend | feature |
| S8.9 | Add reader state persistence tests | frontend | test |

## Verification

- Read to the middle, close, reopen on another browser profile, confirm resume.
- Break an asset reference and confirm the reader still shows the article.
- Keyboard-only read-through of an article.

## Risks

- Character offsets destabilize if the reader payload changes; tie position to
  the artifact version and prefer anchors with percentage fallback.
