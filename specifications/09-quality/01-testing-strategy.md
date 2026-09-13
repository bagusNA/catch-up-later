# Testing Strategy

## Backend tests

### Unit tests

- Domain policies.
- Status transitions.
- Ownership checks.
- Idempotency behavior.
- Package validation.
- Metadata normalization.
- Reading progress calculations.

### Integration tests

- SQLite migrations.
- Repository behavior.
- Artifact storage.
- Capture processing.
- Search indexing.
- API authorization.
- Multipart or package upload.
- Transaction and cleanup behavior.

### Security tests

- XSS payload sanitization.
- Dangerous URL removal.
- Path traversal.
- Oversized uploads.
- Invalid MIME/file signatures.
- Cross-user access.
- Unauthorized artifact access.
- SSRF regression tests.

## Extension tests

- Adapter detection.
- Fixture extraction.
- Metadata extraction.
- Asset collection.
- Package generation.
- Sanitization.
- Retry queue.
- Idempotency.
- Backend error handling.

## Frontend tests

- Library filtering.
- Reader rendering.
- Reading-state persistence.
- Loading/error/empty states.
- Keyboard navigation.
- Accessibility checks.
- Artifact and asset failures.

## End-to-end scenarios

1. Save a generic article and read it.
2. Save a Wikipedia article with images and references.
3. Save a Medium article.
4. Save a PDF.
5. Retry after backend outage.
6. Retry after temporary upload failure.
7. Reject invalid HTML.
8. Reject oversized PDF.
9. Search article body text.
10. Search extracted PDF text.
11. Open the same library from two devices.
12. Confirm one user cannot access another user's content.
13. Delete an item and verify artifact cleanup.
14. Backup and restore a library.
