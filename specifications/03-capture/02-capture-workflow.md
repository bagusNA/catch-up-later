# Capture Workflow

## Happy path

1. User opens a supported page.
2. User clicks Save in the extension.
3. Extension selects the highest-priority applicable adapter.
4. Adapter captures content from the browser context.
5. Adapter extracts metadata and content.
6. Extension sanitizes article HTML.
7. Extension packages content and assets.
8. Extension submits the package.
9. Backend creates a capture job.
10. Backend validates and sanitizes the package.
11. Backend stores immutable artifact files.
12. Backend creates or updates the content item.
13. Backend indexes searchable text.
14. Backend marks the capture `READY`.
15. Extension displays success.

## Failure behavior

Failures MUST be categorized:

- `UNSUPPORTED_SOURCE`
- `EXTRACTION_FAILED`
- `INVALID_PACKAGE`
- `UPLOAD_FAILED`
- `AUTHENTICATION_FAILED`
- `STORAGE_FAILED`
- `PROCESSING_FAILED`
- `ASSET_LIMIT_EXCEEDED`
- `CONTENT_TOO_LARGE`
- `RATE_LIMITED`
- `UNKNOWN`

The extension MUST show a retry action for retryable failures.

## Retry rules

- Retryable network failures use exponential backoff.
- The extension MUST avoid creating duplicate content items on retry.
- The backend MUST accept an idempotency key for each capture attempt.
- The same idempotency key and owner MUST return the same capture result.
- A retry after a permanent validation failure MUST require a new package or explicit user action.
- The extension SHOULD retain failed packages locally until the user dismisses them or storage limits are reached.

## Capture status polling

The extension MAY poll a capture-job endpoint. The backend SHOULD also support a future push mechanism, but push is not required for MVP.

## Capture quality

The result should expose:

- Success or failure.
- Warnings.
- Adapter used.
- Artifact type.
- Number of assets captured.
- Missing asset count.
- Whether full text was extracted.
- Whether the artifact is ready to read.
