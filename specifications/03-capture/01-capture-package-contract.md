# Capture Package Contract

## Purpose

The capture package is the versioned contract between the browser extension and backend. It MUST be independent of database schemas and frontend implementation details.

## Package requirements

The package MUST contain:

- `schemaVersion`
- Source information
- Artifact type
- Capture metadata
- Extracted metadata
- Normalized content
- Asset manifest
- Warnings, if any

## Illustrative package

```json
{
  "schemaVersion": 1,
  "source": {
    "url": "https://example.com/article",
    "canonicalUrl": "https://example.com/article",
    "sourceName": "Example",
    "adapterId": "generic-readability",
    "adapterVersion": "1.0.0"
  },
  "artifact": {
    "type": "ARTICLE_READER",
    "title": "Example article",
    "html": "<article>...</article>",
    "text": "Example article text",
    "language": "en",
    "readingTimeMinutes": 8
  },
  "metadata": {
    "author": "Author",
    "description": "Description",
    "siteName": "Example",
    "publishedAt": "2026-01-01T00:00:00Z",
    "image": {
      "assetKey": "asset-001"
    }
  },
  "assets": [
    {
      "assetKey": "asset-001",
      "mimeType": "image/jpeg",
      "originalUrl": "https://example.com/image.jpg",
      "altText": "Description",
      "content": "<binary multipart part>"
    }
  ],
  "capture": {
    "capturedAt": "2026-09-12T10:00:00Z",
    "warnings": []
  }
}
```

The binary transport format MAY be multipart, a ZIP package, or JSON metadata plus binary upload parts. The chosen format MUST support large PDFs and assets without requiring unsafe memory usage.

## Required semantics

- `source.url` is the URL visible to the user at capture time.
- `canonicalUrl` is optional and must not replace the original URL.
- `adapterId` identifies the capture strategy.
- `artifact.type` identifies the reader/storage type.
- `html` is initial sanitized HTML for article artifacts.
- `text` is plain text used for search and reading-time calculations.
- Asset references in HTML MUST use package-local asset keys, not arbitrary filesystem paths.
- PDF packages MUST include the original PDF bytes.
- Metadata fields are optional unless required by the artifact type.
- Unknown fields SHOULD be ignored for forward compatibility.
- Unsupported schema versions MUST be rejected with a clear error.

## Backend trust model

The backend MUST independently:

1. Authenticate the user.
2. Validate package schema.
3. Validate artifact type.
4. Re-sanitize HTML.
5. Validate asset references.
6. Validate MIME types and file signatures.
7. Enforce size limits.
8. Calculate checksums.
9. Normalize metadata.
10. Store only validated content.
