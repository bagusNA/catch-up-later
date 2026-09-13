# Slice 2 — Save Generic Article → Read

**Milestone:** `S2 Core capture`
**Outcome:** The walking skeleton. A user saves an ordinary web article from the
extension, the backend validates and stores an immutable artifact, and the
article appears in the Library and opens in the reader.
**Depends on:** `S1`.

## Goal

Prove the full capture pipeline end-to-end for the generic Readability adapter
before adding adapters or library features.

## In scope

- Extension adapter framework and package builder (ZIP, schema version 1).
- Generic Readability adapter: extraction, metadata, sanitization, reading time.
- Asset collection, local rewriting, count/size limits, warnings.
- Popup save action, background upload, basic capture status.
- Backend capture intake, job persistence, validation, re-sanitization, storage.
- Content item creation and the minimal reader endpoint.
- Frontend minimal Library and article reader.

## Out of scope

- Library search/filter/sort (`S3`), retry queue (`S4`), other adapters
  (`S5`–`S6`), PDFs (`S7`), reader controls/resume (`S8`).

## Acceptance criteria

- [x] Saving an ordinary article in the extension returns a `captureId` and
      eventually `READY`.
- [x] The backend independently rejects invalid packages, re-sanitizes HTML,
      validates MIME signatures, and enforces size/count limits.
- [x] Stored artifacts are immutable under a generated, owner-scoped path;
      writes use staging + atomic commit.
- [x] The article appears in the Library and renders with local images, no
      script execution, and no dependency on the source site.
- [x] A capture with missing assets still becomes `READY` with warnings.
- [x] `GET /api/v1/content-items/{id}/reader` and asset endpoints enforce
      ownership.
- [x] The capture package schema is documented and versioned.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S2.1 | Add capture package schema v1 types and ZIP builder | extension | feature |
| S2.2 | Implement adapter framework (contract, priority selection, composition) | extension | feature |
| S2.3 | Implement GenericReadabilityAdapter (extract, metadata, reading time) | extension | feature |
| S2.4 | Implement asset collection, rewriting, limits, and warnings | extension | feature |
| S2.5 | Implement popup save action and background upload with idempotency key | extension | feature |
| S2.6 | Add capture jobs table and `POST /api/v1/captures` multipart intake | backend | feature |
| S2.7 | Implement package validation (schema, type, MIME/signature, limits) | backend | feature |
| S2.8 | Implement HTML re-sanitization policy independent of the extension | backend | feature |
| S2.9 | Implement `ArtifactStorage` staging/commit and filesystem layout | backend | feature |
| S2.10 | Create content item and artifact version on successful processing | backend | feature |
| S2.11 | Add `GET /api/v1/content-items/{id}/reader` and manifest endpoint | backend | feature |
| S2.12 | Add ownership-checked asset serving endpoint | backend | feature |
| S2.13 | Add `GET /api/v1/captures/{captureId}` status endpoint | backend | feature |
| S2.14 | Build minimal Library list with capture status | frontend | feature |
| S2.15 | Build article reader with sanitized isolated surface | frontend | feature |
| S2.16 | Add end-to-end test: save generic article and read it | backend | test |
| S2.17 | Add adapter fixture tests (recognition, extraction, malformed input) | extension | test |

## Demo

1. Start the backend (`./gradlew bootRun --args='--spring.profiles.active=local'`),
   frontend (`pnpm dev`), and extension (`pnpm dev`).
2. Connect the extension to the backend in its options page, sign in, open an
   article, and click **Save this page**. The popup reports `Saved` and the
   article appears in `/library`.
3. Confirm the reader renders images without network access to the source site.

Curl equivalent (replace the token and package path):

```bash
curl -X POST http://localhost:8080/api/v1/captures \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -F "package=@capture.zip;type=application/zip"
```

## Verification

- Manual: save three different article sites, confirm all render offline.
- Automated: package validation rejects a tampered archive and a script payload.
- Stored artifact bytes and reader HTML are byte-stable across reads.

## Risks

- Readability failures on complex pages; surface as categorized failure, do not
  claim success.
- Asset volume can bloat packages; limits must fail closed with a warning.
