# UI Guidelines

## 1. Product Direction

Catch Up Later is a private, self-hosted content library for curious people who save articles and PDFs to read later.

The experience should feel like a **personal reading room**, not a productivity dashboard or social platform.

### Core promise

> Save something interesting now. Read it later in a clean, focused environment.

### MVP content

- Generic web articles captured through Mozilla Readability.
- Wikipedia articles.
- Medium articles where capture is possible.
- PDF documents.
- No public sharing, collaboration, AI summaries, video/audio, EPUB, or social features.

## 2. Design Principles

### Calm and content-first
- Prioritize saved content over application chrome.
- Use generous whitespace and restrained hierarchy.
- Avoid dashboards, gamification, noisy animations, and excessive badges.
- Make reading the primary action.

### Fast to understand
- Users should immediately understand how to save, find, and read.
- Use familiar labels: Library, Inbox, Read, In Progress, Favorites, Tags.
- Prefer clear text actions over ambiguous icons.

### Honest states
Clearly distinguish:
- Ready to read.
- Processing.
- Failed.
- Saved with warnings.
- Missing optional assets.
- Text extraction unavailable.

Never present a partial or failed capture as fully successful.

### Private by default
- No social affordances in MVP.
- No sharing, likes, comments, or public profiles.
- Make personal ownership clear.

## 3. Visual Direction

- Modern, editorial, understated.
- Neutral or warm-neutral surfaces.
- Strong, highly legible typography.
- Limited accent color for actions and selection.
- Light and dark themes.
- Subtle borders and elevation; avoid heavy shadows and gradients.
- Responsive from mobile to desktop.
- Constrained reading column with wider treatment for media and tables.
- Visible focus states and keyboard accessibility.
- Do not rely on color alone for status.
- Support reduced motion.

## 4. Information Architecture

Primary navigation:
- Library
- Favorites
- Tags
- Settings

Processing and failed items are shown inline in the Library; there is no separate inbox.

Library organization:
- Reading status: Unread, In Progress, Read.
- Favorite state.
- Tags.
- Content type: Article or PDF.
- Saved date.
- Last-read date.
- Search.

Avoid making folders the primary organization mechanism in MVP.

## 5. Screens to Design

### Authentication and onboarding

1. **Login**
   - Login fields, submit, loading, error, instance context.
2. **Initial setup / onboarding**
   - Short save → capture → read explanation.
   - Extension installation/configuration guidance.
   - Empty-library state.

### Library

3. **Home**
   - This week reading statistics
   - Continue reading
   - Recently added 
   - Might be interested section (random content)
4. **Library**
   - Search, content-type filter, status filter, favorite filter, tag filter, sort.
   - List/grid toggle if supported.
   - Item count, empty state, loading state, error state.
   - Recent captures.
   - Processing and failed items.
   - Retry actions.
   - Empty state.
5. **Favorites**
   - Favorite items with the same browsing patterns as Library.
6. **Bookmark**
   - Content bookmark, user-chosen section specific (e.g. quote or article section)
7. **Search results**
   - Query, result count, filters, ranking, matched-term highlighting where practical.
   - No-results state.

### Content item

8. **Content item detail**
   - Title, source, original URL, type, author/date when available, capture date.
   - Status, favorite, tags, read/continue action, delete.
   - Capture warnings and artifact/version information where useful.
9. **Capture processing detail**
   - Current status, progress/message, retry, error explanation, cancel/delete.

### Reader

10. **Article reader**
    - Clean reading surface.
    - Title and metadata.
    - Source/original link.
    - Capture date and reading time.
    - Article body and local images.
    - Reading progress and resume position.
    - Font size, content width, and theme controls.
    - Status, favorite, back-to-library, secondary actions.
11. **PDF reader**
    - PDF rendering, page navigation, zoom, current page.
    - Search when extracted text exists.
    - Reading progress, download/export, original source.
12. **Reader degraded/error states**
    - Artifact unavailable or invalid.
    - Missing assets.
    - PDF rendering failure.
    - Text search unavailable.
    - Original source unavailable.
    - Partial capture warnings.

### Settings and operations

13. **Account/settings**
    - Account, theme, reader defaults, extension connection, logout.
14. **Reader preferences**
    - Default font size, width, theme, optional line-height.
    - May be a panel rather than a route.
15. **Storage/system information**
    - Storage usage, artifact count, system version, backup guidance/status.
16. **Destructive-action dialogs**
    - Delete item, delete artifact/version, remove tag, logout, clear failed capture.

## 6. Shared Components to Design

### Application shell
- Sidebar/responsive navigation.
- Mobile navigation.
- Header.
- Page container.
- Account menu.
- Global toast/notification area.

### Library
- Content item card.
- Content item list row.
- Content-type badge.
- Reading-status badge/control.
- Favorite button.
- Tag chip.
- Search bar.
- Filter bar.
- Sort menu.
- View toggle.
- Pagination or infinite-loader.
- Empty, loading, and error states.

### Capture
- Save/capture status indicator.
- Processing status row.
- Capture failure card.
- Retry button.
- Capture warning display.
- Duplicate-content notice.
- Extension connection/setup prompt.

### Reader
- Reader shell.
- Reader toolbar.
- Reader settings popover.
- Reading progress bar.
- Article metadata block.
- Article typography styles.
- Source attribution block.
- PDF toolbar.
- PDF page navigator.
- PDF search control.
- Resume-reading prompt.
- Reader error/degraded state.

### Forms and dialogs
- Login form.
- Tag picker.
- Create/rename tag form.
- Status selector.
- Delete confirmation dialog.
- Settings controls.
- Toast/notification.
- Confirm/cancel action group.

## 7. Required Component States

Design all applicable components in:
- Default.
- Hover.
- Focus.
- Pressed.
- Disabled.
- Loading.
- Error.
- Empty.
- Mobile/responsive.
- Dark theme.

Complete state coverage is especially important for:
1. Saving an article.
2. Saving a PDF.
3. Capture processing.
4. Capture failure and retry.
5. Partial capture with warnings.
6. Empty library.
7. Search with no results.
8. Reader with missing assets.
9. Reader resume position.
10. Delete confirmation.

## 8. Key UX Flows

### Save and read
Open webpage → click extension Save → capture processing → ready → open item → read → resume later → mark read.

### Failed capture
Click Save → capture fails → show reason → retry → processing → ready or actionable failure.

### Find something later
Open Library → search/filter → open content item → continue reading.

### PDF reading
Save PDF → processing → ready → open PDF reader → navigate/search/read → resume later.

## 9. Design Deliverables

- Desktop and mobile layouts for all primary screens.
- Light and dark theme variants.
- Component library with states.
- Reader typography specification.
- Responsive behavior rules.
- Empty, loading, error, and degraded states.
- Interaction notes for capture, retry, filtering, and reading progress.
- Accessibility notes.
- Design tokens for color, typography, spacing, radius, and elevation.
- Handoff-ready component names and variants.

## 10. Design Priorities

1. Article reader.
2. Main library.
3. Capture processing and failure states.
4. Content item detail.
5. PDF reader.
6. Search and filtering.
7. Authentication/onboarding.
8. Settings and storage.
9. Secondary tag and favorite views.
