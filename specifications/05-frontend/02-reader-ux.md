# Reader UX

## Reader goals

The reader should feel like a quiet reading surface, not a webpage viewer.

## Article reader

Required:

- Article title.
- Source and original link.
- Author and publication date when available.
- Capture date.
- Estimated reading time.
- Article body.
- Local images.
- Reading progress.
- Resume position.
- Font size control.
- Content width control.
- Theme control.
- Status control.
- Back-to-library action.

Recommended defaults:

- Comfortable max line length.
- Generous line height.
- No sidebar by default.
- Sticky but collapsible reader controls.
- Preserve heading hierarchy.
- Preserve meaningful tables and lists.

## PDF reader

Required:

- Original PDF rendering.
- Page navigation.
- Zoom.
- Current page indicator.
- Search when extracted text is available.
- Reading progress.
- Download/export original file.
- Original source link.

## Reading progress

Article progress SHOULD use a stable position representation. A plain character offset is simple but can become unstable if the reader representation changes. Prefer a combination of:

- Artifact ID.
- Progress percentage.
- Text offset or anchor.
- Last viewport position.

PDF progress MUST include page number.

## Status behavior

- Opening an unread item does not necessarily mark it read immediately.
- After meaningful progress, status becomes `IN_PROGRESS`.
- The user can explicitly mark an item read.
- Reaching the end MAY suggest marking it read.
- Users can revert to unread.

## Error states

The reader MUST distinguish:

- Artifact unavailable.
- Artifact invalid.
- Missing asset.
- Search text unavailable.
- PDF text extraction unavailable.
- Original source unavailable.

The reader should still display usable content when optional assets or text extraction are missing.
