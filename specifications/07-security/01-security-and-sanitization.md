# Security and Content Sanitization

## Threat model

The system stores untrusted web content supplied by users or source pages. Malicious content may attempt:

- XSS in the reader.
- Script execution.
- Credential theft.
- SSRF through backend asset fetching.
- Path traversal.
- Resource exhaustion.
- Malicious PDF exploitation.
- Metadata injection.
- Cross-user access.

## HTML rules

The backend MUST:

- Sanitize HTML independently of the extension.
- Remove scripts.
- Remove event-handler attributes.
- Remove dangerous URLs such as `javascript:`.
- Restrict or remove iframes unless explicitly supported.
- Remove forms and interactive controls unless explicitly supported.
- Normalize links.
- Validate image and asset references.
- Apply a restrictive Content Security Policy.
- Render article content in an isolated context where practical.

## Backend request security

- Authenticate every private endpoint.
- Authorize by owner ID.
- Use parameterized SQL.
- Validate all input sizes.
- Enforce upload limits before processing.
- Avoid logging full article content or credentials.
- Rate-limit capture endpoints.
- Use CSRF protection when cookie authentication is used.
- Validate idempotency keys.

## Filesystem security

- Never use user input as a raw path.
- Store artifacts under generated IDs.
- Reject path traversal sequences.
- Validate file signatures, not only declared MIME types.
- Keep staging files outside public static directories.
- Serve artifacts through authorization-checked endpoints.

## SSRF policy

The backend MUST NOT fetch arbitrary URLs from capture packages in the MVP. Asset fetching occurs in the extension. Any future server-side fetching must use an explicit allowlisted, sandboxed design.

## PDF security

- Validate PDF signatures.
- Enforce file-size and page-count limits where practical.
- Do not execute embedded scripts.
- Use a maintained PDF rendering library.
- Treat extracted text as untrusted content.
