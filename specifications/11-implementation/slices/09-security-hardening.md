# Slice 9 — Security Hardening

**Milestone:** `S9 Security`
**Outcome:** The threat model in `07-security/01-security-and-sanitization.md`
is implemented and verified with regression tests.
**Depends on:** `S2`–`S8`.

## Goal

Audit every surface introduced by the MVP and close the gaps with automated
security tests.

## In scope

- Backend re-sanitization policy review and CSP for the reader.
- Isolated reader surface (sandboxed rendering, no inline handlers).
- Rate limiting on auth and capture endpoints.
- Request size limits enforced before parsing.
- MIME and file-signature validation audit.
- Path traversal and ownership regression suite.
- SSRF policy verification (no server-side fetching).
- Secret handling and redacted logging review.

## Out of scope

- Penetration testing by an external party.
- WAF/edge controls (deployment responsibility).

## Acceptance criteria

- [ ] XSS payloads (script tags, event handlers, `javascript:` URLs) are removed
      both at packaging and at backend storage.
- [ ] The reader renders in an isolated context with a restrictive CSP and no
      inline script execution.
- [ ] Path traversal attempts cannot address files outside the artifact root.
- [ ] Oversized uploads are rejected before full processing.
- [ ] Invalid MIME/file signatures are rejected for assets and PDFs.
- [ ] A user cannot read, search, download, or delete another user's data.
- [ ] Capture and auth endpoints are rate-limited with a clear `RATE_LIMITED`
      response.
- [ ] Logs contain no passwords, tokens, full HTML, or document contents.

## Tasks

| ID | Task | Area | Type |
|----|------|------|------|
| S9.1 | Review and pin the backend sanitization policy | backend | hardening |
| S9.2 | Add reader Content Security Policy and sandbox hardening | frontend | hardening |
| S9.3 | Add rate limiting for auth and capture endpoints | backend | hardening |
| S9.4 | Enforce and test upload/request size limits before parsing | backend | hardening |
| S9.5 | Audit MIME/file-signature validation for assets and PDFs | backend | hardening |
| S9.6 | Add path traversal regression suite | backend | test |
| S9.7 | Add cross-user access regression suite for all resources | backend | test |
| S9.8 | Add SSRF policy regression test (no server-side fetch) | backend | test |
| S9.9 | Audit log redaction for secrets and content | backend | hardening |
| S9.10 | Add XSS sanitization regression fixtures | extension | test |

## Verification

- Run the full security suite in CI.
- Manually inject a known XSS payload through a capture package and confirm it
  is neutralized at rest and at render.

## Risks

- Over-sanitizing breaks complex articles; keep a permissive-but-safe allowlist
  and cover it with fixtures.
