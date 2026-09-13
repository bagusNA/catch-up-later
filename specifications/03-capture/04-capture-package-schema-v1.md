# Capture Package Schema v1

This document is the normative, versioned description of the ZIP capture
package introduced by `S2`. It refines `01-capture-package-contract.md` with
the concrete transport chosen in DEC-004.

## Transport

A package is a single ZIP archive uploaded as the `package` multipart part of
`POST /api/v1/captures`.

```text
manifest.json
content.html
content.txt
assets/{assetKey}
```

- `manifest.json` — UTF-8 JSON metadata, described below.
- `content.html` — the normalized, sanitized article HTML.
- `content.txt` — the plain text used for reading time and future search.
- `assets/{assetKey}` — binary assets referenced by the HTML and manifest.

Entry names are fixed; the reader of the archive must reject absolute paths,
backslashes, `..` segments, and duplicate entries.

## Manifest

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
    "language": "en",
    "readingTimeMinutes": 8
  },
  "metadata": {
    "author": "Author",
    "description": "Description",
    "siteName": "Example",
    "publishedAt": "2026-01-01T00:00:00Z",
    "image": { "assetKey": "asset-1" }
  },
  "assets": [
    {
      "assetKey": "asset-1",
      "mimeType": "image/png",
      "originalUrl": "https://example.com/image.png",
      "altText": "Description",
      "byteSize": 12345,
      "checksum": "sha256-hex",
      "width": 640,
      "height": 480
    }
  ],
  "capture": {
    "capturedAt": "2026-09-12T10:00:00Z",
    "warnings": [{ "code": "ASSET_MISSING", "message": "…" }]
  }
}
```

### Field rules

- `schemaVersion` must be `1`. Unknown versions are rejected with
  `CAPTURE_INVALID_PACKAGE`.
- `source.url` is required and must be `http`/`https`.
- `source.canonicalUrl` is optional and never replaces `source.url`.
- `artifact.type` must be a known artifact type. `PDF_DOCUMENT` is reserved for
  `S7`; this version of the backend supports `ARTICLE_READER`.
- `artifact.title` is required after normalization.
- Asset keys match `^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$` and are unique.
- `assets[].mimeType` must be one of `image/png`, `image/jpeg`, `image/gif`,
  `image/webp`. SVG is rejected. Declared `byteSize`/`checksum`, when present,
  must match the bytes actually received.
- Unknown fields are ignored for forward compatibility.

## Reserved asset host

Images in `content.html` reference captured assets through the reserved,
never-resolvable host:

```html
<img src="https://assets.cul.invalid/asset-1" alt="…">
```

The backend sanitizer only preserves `img` sources on this host and the reader
rewrites them to ownership-checked URLs:

```text
/api/v1/artifacts/{artifactId}/assets/{assetKey}
```

External `http(s)` image URLs are stripped during backend re-sanitization, so a
stored artifact never depends on the source site.

## Trust model

The extension sanitizes with DOMPurify before packaging, but the backend treats
every package as untrusted and independently:

1. validates the schema and artifact type,
2. re-sanitizes HTML with the pinned OWASP policy,
3. verifies asset signatures (not just declared MIME types),
4. enforces per-entry, per-asset, and total size limits,
5. computes authoritative checksums,
6. stores only validated content under a generated, owner-scoped storage key.
