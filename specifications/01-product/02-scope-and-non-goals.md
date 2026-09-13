# MVP Scope and Non-Goals

## In scope

### Content

- Generic web articles captured with Mozilla Readability.
- Wikipedia articles.
- Medium articles where capture is possible from the user's browsing context.
- PDF files captured through the extension.
- Immutable artifact storage.
- Article assets required for local rendering.
- PDF original-file retention.
- Full-text search over article text and extracted PDF text where available.

### Library

- List and grid views.
- Search.
- Filter by content type.
- Filter by reading status.
- Tags.
- Favorites.
- Sort by saved date, title, and last-read date.
- Item deletion.
- Artifact and metadata inspection.

### Reader

- Distraction-free article reader.
- PDF viewer.
- Light/dark theme.
- Font size and content-width controls for articles.
- Reading progress.
- Resume position.
- Estimated reading time for articles.
- Original-source link.
- Read/in-progress/unread status.

### Capture

- Browser extension save action.
- Source detection.
- Adapter-based capture.
- Capture package creation.
- Initial client-side sanitization.
- Upload to backend.
- Retry for failed captures.
- Capture status reporting.
- Duplicate detection.

### Operations

- Docker-based deployment.
- SQLite persistence.
- Filesystem artifact storage.
- Health endpoint.
- Structured logs.
- Backup and restore guidance.

## Explicit non-goals

- General server-side crawling.
- Automatic refresh for arbitrary websites.
- Bypassing paywalls, DRM, authentication, or access controls.
- Public sharing.
- Collaboration.
- AI summaries or recommendations.
- Video/audio downloading.
- EPUB support.
- Full-fidelity website archival.
- Browser history ingestion.
- Mobile-native application.
- General-purpose user plugin runtime.
- Social features.
- Automatic source discovery feeds.
