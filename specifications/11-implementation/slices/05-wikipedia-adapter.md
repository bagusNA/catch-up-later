# Slice 5 — Wikipedia Adapter

**Milestone:** `S5 Wikipedia`
**Outcome:** Wikipedia pages capture with high fidelity and fall back safely.
**Depends on:** `S2`.

## Goal

Add the first specialized adapter and prove the fallback pattern.

## In scope

- Wikipedia domain/URL recognition.
- Specialized extraction preserving headings, tables, references, images, links.
- Fallback to the generic pipeline with a recorded warning.
- Refresh capability flag reported as disabled.

## Out of scope

- Wikipedia refresh implementation (deferred).
- Other adapters.

## Acceptance criteria

- [ ] Wikipedia article URLs select the Wikipedia adapter by priority.
- [ ] Headings, tables, references, and images survive capture and render.
- [ ] Non-article Wikipedia URLs fall back to generic Readability with a warning.
- [ ] A malformed or partial page produces a categorized failure, not a silent
      low-quality artifact.
- [ ] Fixture tests cover recognition, metadata, assets, malformed input, and
      fallback.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S5.1 | Implement Wikipedia detector and adapter | extension | feature |
| S5.2 | Preserve tables, references, and figure assets | extension | feature |
| S5.3 | Implement generic fallback with warning | extension | feature |
| S5.4 | Report refresh capability as disabled | extension | feature |
| S5.5 | Add Wikipedia fixture tests | extension | test |
| S5.6 | Add an end-to-end Wikipedia capture test | backend | test |

## Verification

- Capture a long, image-heavy article and a reference-heavy article; compare
  rendered structure to the source headings and tables.
- Confirm fallback path recorded when a non-article URL is saved.

## Risks

- Wikipedia markup changes; keep a small, well-tested extraction surface and
  rely on fallback.
