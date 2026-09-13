# Browser Extension Architecture

## Responsibilities

The extension is responsible for:

- User-initiated capture.
- Source detection.
- Browser-context extraction.
- Initial sanitization.
- Asset collection.
- Package creation.
- Upload.
- Retry queue.
- Capture status display.
- Backend authentication configuration.

The extension MUST NOT contain database persistence logic or backend-specific domain rules beyond package compatibility.

## WebExtension components

Suggested components:

- Content script: page inspection and DOM capture.
- Background service worker: orchestration, upload, retry queue, authentication.
- Popup: current-page save action and status.
- Options page: backend URL, account connection, capture preferences.
- Shared adapter library: source detection and capture strategies.

## Capture context

The context SHOULD include:

- Current tab URL.
- Canonical URL if available.
- Document title.
- DOM snapshot or cloned document.
- Page metadata.
- Selected text if user capture mode supports it.
- Browser locale.
- Current timestamp.
- Source permissions and capture limitations.

## Asset collection

- Resolve relative URLs against the source page.
- Capture only assets referenced by the normalized artifact.
- Enforce count and size limits.
- Preserve MIME type and original URL.
- Replace HTML references with package-local asset keys.
- Report missing assets as warnings.

## Authentication

- The extension MUST communicate with the configured backend over HTTPS outside local development.
- Credentials MUST NOT be injected into page content.
- Tokens MUST be stored using extension-secure storage mechanisms.
- The extension MUST support logout and backend URL changes.
- The extension MUST not send cookies or session tokens from arbitrary websites to the backend.

## Retry queue

Each queued capture should store:

- Local job ID.
- Idempotency key.
- Backend URL.
- Package reference.
- Attempt count.
- Last error.
- Next retry time.
- User-visible status.

The queue MUST be bounded and expose storage limits.

## Permissions

Request the minimum permissions needed. Host permissions should be scoped where practical. The extension should clearly explain why capture permissions are needed.
