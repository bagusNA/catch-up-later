# Frontend Architecture

## Stack

- Vue
- Nuxt
- Nuxt UI
- TypeScript
- A typed API client
- State management only where shared state requires it

## Main routes

- `/login`
- `/library`
- `/inbox`
- `/content/[id]`
- `/content/[id]/read`
- `/settings`
- `/settings/storage`
- `/settings/account`

## UI principles

- Calm, content-first interface.
- Minimal dashboard chrome.
- Responsive layout.
- Keyboard accessible.
- Clear capture and processing states.
- No reader dependency on the original website.
- Avoid unnecessary animations.

## Main components

- `LibraryToolbar`
- `ContentItemCard`
- `ContentItemList`
- `ContentFilters`
- `CaptureStatus`
- `ArticleReader`
- `PdfReader`
- `ReaderControls`
- `ReadingProgress`
- `TagPicker`
- `EmptyState`
- `ErrorState`

## Data fetching

- Use a typed API client.
- Handle loading, empty, error, and stale states explicitly.
- Mutations should optimistically update only when rollback is reliable.
- Reader content should be fetched by artifact ID or content item ID through ownership-checked endpoints.

## Content rendering

Article HTML MUST be rendered in an isolated reader surface with:

- Sanitized backend output only.
- No script execution.
- No inline event handlers.
- Safe asset URLs.
- Restricted external navigation.
- A clear boundary between application UI and article content.

## Accessibility

- Keyboard navigation.
- Visible focus states.
- Semantic headings.
- Proper button labels.
- Sufficient contrast.
- Screen-reader labels for reading controls.
- Reader progress announced where useful.
