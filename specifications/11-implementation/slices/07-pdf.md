# Slice 7 — PDF End-to-End

**Milestone:** `S7 PDF`
**Outcome:** A user saves a PDF, it is validated, stored, text-extracted,
indexed, and readable with page navigation and search.
**Depends on:** `S2` (and benefits from `S3` for search).

## Goal

Extend the capture and reader pipeline to the second MVP content type.

## In scope

- Extension PDF detection and raw byte capture.
- Backend PDF signature/size/page validation and immutable storage.
- Server-side text extraction (Apache PDFBox) with graceful failure.
- PDF reader manifest/file/download endpoints.
- Frontend PDF viewer: render, page nav, zoom, progress, download, original link.
- PDF text search when extraction succeeded.

## Out of scope

- PDF annotation, editing, or conversion.
- Video/audio and other artifact types (deferred).

## Acceptance criteria

- [ ] Saving a PDF captures the original bytes and becomes `READY`.
- [ ] Invalid signatures and oversized/paged PDFs are rejected with categorized
      errors and no stored artifact.
- [ ] Extracted text is searchable; extraction failure leaves the PDF readable
      with a warning and `pdfTextExtracted: false`.
- [ ] The PDF reader renders locally, supports page navigation and zoom, shows
      the current page, and resumes from the saved page.
- [ ] The original file can be downloaded by the owner only.
- [ ] Ownership and path safety apply to all PDF endpoints.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S7.1 | Implement PDF detection and raw byte capture | extension | feature |
| S7.2 | Extend package builder for PDF payloads | extension | feature |
| S7.3 | Add PDF validation (signature, size, page count) | backend | feature |
| S7.4 | Add Apache PDFBox text extraction with failure warnings | backend | feature |
| S7.5 | Add PDF artifact storage and manifest | backend | feature |
| S7.6 | Add `GET /api/v1/artifacts/{id}/file` and download endpoint | backend | feature |
| S7.7 | Build PDF reader with render, page nav, zoom, current page | frontend | feature |
| S7.8 | Add PDF progress (page) persistence and resume | frontend | feature |
| S7.9 | Add PDF text search UI when text is available | frontend | feature |
| S7.10 | Add PDF validation, extraction, and ownership tests | backend | test |

## Verification

- Capture a large, a scanned (no text), and a malformed PDF.
- Confirm search finds text in the text PDF and reports unavailability otherwise.
- Confirm a non-owner cannot fetch the file.

## Risks

- Memory pressure on large PDFs; stream where possible and enforce limits before
  parsing.
- Malicious PDFs; never execute embedded scripts, keep the library updated.
