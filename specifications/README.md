# Catch Up Later — Application Specification

## Purpose

Catch Up Later is a self-hosted, private content library for curious people. Users save web articles and PDFs through a browser extension. The extension captures the content locally, creates a versioned capture package, and uploads it to the user's backend. The backend validates, sanitizes, stores, indexes, and serves immutable artifacts through a distraction-free reader.

## Specification status

This specification describes the MVP and the architectural seams required for future source adapters and content types.

## MVP decisions

- Self-hosted deployment.
- Single-user-first experience, with user ownership modeled for private multi-user support.
- Backend: Spring Boot + Kotlin.
- Frontend: Vue + Nuxt UI + TypeScript.
- Browser extension: standards-based WebExtension.
- Database: SQLite.
- MVP content types: articles and PDFs.
- MVP sources: generic Readability articles, Wikipedia, and Medium.
- Capture is user-initiated through the extension.
- No general server-side fetching or crawling.
- Artifacts are immutable.
- Wikipedia refresh may be supported later as an explicit source-specific capability.
- Capture failures can be retried.
- No sharing, collaboration, AI summaries, video/audio downloading, EPUB, or full website archiving in MVP.

## Reading order

1. `01-product/01-product-overview.md`
2. `01-product/02-scope-and-non-goals.md`
3. `02-domain/01-domain-model.md`
4. `02-domain/02-content-and-artifact-lifecycle.md`
5. `03-capture/01-capture-package-contract.md`
6. `03-capture/02-capture-workflow.md`
7. `03-capture/03-source-adapter-architecture.md`
8. `04-backend/01-backend-architecture.md`
9. `04-backend/02-api-specification.md`
10. `04-backend/03-persistence-and-storage.md`
11. `04-backend/04-search-indexing.md`
12. `05-frontend/01-frontend-architecture.md`
13. `05-frontend/02-reader-ux.md`
14. `06-extension/01-extension-architecture.md`
15. `07-security/01-security-and-sanitization.md`
16. `08-operations/01-deployment-backup-and-observability.md`
17. `09-quality/01-testing-strategy.md`
18. `10-roadmap/01-future-extensibility.md`

## Normative language

- **MUST** means required for conformance.
- **SHOULD** means recommended unless there is a documented reason not to.
- **MAY** means optional.
