# Source Adapter Architecture

## Design goal

Adding a new source MUST be a localized implementation task. Adapters MUST NOT directly access persistence repositories, SQLite, HTTP controllers, or frontend components.

## Adapter responsibilities

An adapter may:

- Recognize a page or document.
- Identify source and content type.
- Capture content from browser context.
- Extract metadata.
- Normalize content.
- Collect assets.
- Produce warnings.
- Declare capabilities.
- Provide deterministic fixture tests.

## Adapter contract

Conceptual TypeScript contract:

```ts
interface SourceAdapter {
  id: string
  version: string
  priority: number
  canHandle(context: CaptureContext): Promise<boolean> | boolean
  capture(context: CaptureContext): Promise<AdapterCaptureResult>
}
```

Conceptual result:

```ts
interface AdapterCaptureResult {
  status: "SUCCESS" | "SUCCESS_WITH_WARNINGS" | "FAILURE"
  source: SourceInfo
  artifact: ArtifactPayload
  metadata: ExtractedMetadata
  assets: CapturedAsset[]
  warnings: CaptureWarning[]
  error?: CaptureError
}
```

The actual implementation may split this into detector, extractor, normalizer, sanitizer, and package-builder interfaces.

## Recommended adapter organization

```text
ArticleCaptureStrategy
  ├── GenericReadabilityAdapter
  ├── WikipediaAdapter
  └── MediumAdapter

DocumentCaptureStrategy
  └── PdfAdapter
```

Prefer composition over deep inheritance.

Shared components SHOULD include:

- URL normalization.
- Metadata extraction helpers.
- Asset URL resolution.
- Asset downloading from the current page context.
- HTML sanitization.
- Relative-link normalization.
- Reading-time calculation.
- Package construction.

## Adapter selection

1. Evaluate adapters in descending priority.
2. Select the first adapter that can handle the context.
3. Allow a specialized adapter to fall back to a shared generic pipeline.
4. Do not fall back to a lower-quality artifact without recording a warning.
5. If no adapter succeeds, return a structured failure.

## MVP adapters

### Generic Readability adapter

- Handles ordinary article pages.
- Uses Mozilla Readability.
- Produces normalized article HTML and plain text.
- Captures referenced images required for rendering.
- Does not guarantee success for every site.

### Wikipedia adapter

- Recognizes supported Wikipedia domains and article URL patterns.
- May use specialized DOM extraction.
- Preserves headings, tables, references, images, and links where practical.
- Can fall back to generic Readability.
- Refresh capability is disabled in MVP unless explicitly implemented later.

### Medium adapter

- Recognizes Medium domains and supported article patterns.
- Uses browser-context capture only.
- Reuses the article normalization pipeline.
- Must not bypass access controls or implement server-side crawling.
- Must report partial or failed capture clearly.

### PDF adapter

- Recognizes PDF documents or browser PDF contexts.
- Captures original PDF bytes.
- Produces PDF artifact metadata.
- May produce extracted text.
- Does not route the PDF through Readability.

## Testing requirements

Every adapter MUST have fixture-based tests for:

- Recognition.
- Metadata extraction.
- Successful capture.
- Missing optional metadata.
- Asset handling.
- Malformed input.
- Unsupported input.
- Sanitization.
- Warnings.
- Fallback behavior.
