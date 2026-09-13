# Slice 6 — Medium Adapter

**Milestone:** `S6 Medium`
**Outcome:** Medium articles capture from the user's browser context, with clear
handling of partial or blocked captures.
**Depends on:** `S2`.

## Goal

Add the second specialized adapter, reusing the article normalization pipeline,
without bypassing access controls.

## In scope

- Medium domain/URL recognition.
- Browser-context extraction only.
- Reuse of shared article normalization, metadata, and sanitization.
- Explicit partial/failed reporting.

## Out of scope

- Paywall bypass, authentication bypass, or server-side crawling (non-goals).

## Acceptance criteria

- [ ] Medium article URLs select the Medium adapter by priority.
- [ ] Capture works only from content available to the logged-in browser session.
- [ ] Partial captures (missing member-only body) fail or warn explicitly.
- [ ] No request is ever made server-side to fetch Medium content.
- [ ] Fixture tests cover recognition, metadata, partial, and blocked content.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S6.1 | Implement Medium detector and adapter | extension | feature |
| S6.2 | Reuse shared metadata and normalization pipeline | extension | feature |
| S6.3 | Detect and report partial/blocked content | extension | feature |
| S6.4 | Add Medium fixture tests | extension | test |
| S6.5 | Add an end-to-end Medium capture test | backend | test |

## Verification

- Capture a free Medium article end-to-end.
- Confirm a member-only page with no accessible body reports an honest partial
  or failure state.

## Risks

- Medium page structure changes frequently; keep extraction thin and prefer
  shared article normalization.
