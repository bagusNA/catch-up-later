# Future Extensibility

## Source adapters

Future adapters may support:

- Substack.
- Documentation sites.
- Academic repositories.
- News sites.
- Podcast pages.
- Video platforms.
- EPUB.
- Specialized document sources.

Each adapter should implement the existing capture contract and should not require changes to core persistence.

## Content types

Potential future artifact types:

- `VIDEO_REFERENCE`
- `AUDIO_REFERENCE`
- `TRANSCRIPT`
- `EPUB_DOCUMENT`
- `IMAGE_DOCUMENT`
- `WEB_ARCHIVE`

New types should provide:

- Capture strategy.
- Validation rules.
- Reader capability.
- Search extraction strategy.
- Progress representation.
- Export behavior.

## Reader renderers

Use a renderer registry or capability mapping:

```text
artifactType → reader renderer
```

The frontend should not contain source-specific URL checks. It should select a reader based on the artifact type and declared capabilities.

## Refresh

Refresh should be an explicit operation that:

1. Checks whether the source supports refresh.
2. Performs an authorized capture strategy.
3. Creates a new immutable artifact version.
4. Preserves the previous version.
5. Updates the current artifact pointer only after success.

Generic server-side refresh remains disabled by default.

## User extensions

A future extension system may allow:

- Custom source adapters.
- Custom metadata enrichers.
- Custom extraction rules.
- Custom reader renderers.
- Import/export adapters.
- Automation hooks.

Do not implement arbitrary code execution or third-party plugin loading in MVP. Start with internal interfaces, versioned contracts, and a stable registry.

## Portability

Future export/import should preserve:

- Content item metadata.
- Artifact versions.
- Assets.
- Tags.
- Reading state.
- Source URLs.
- Adapter and schema metadata.
