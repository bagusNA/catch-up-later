# Search and Indexing

## MVP search scope

Search MUST cover:

- Content item title.
- Description.
- Source name.
- Article plain text.
- Extracted PDF text where available.
- User tags.

Search MAY cover author and canonical URL.

## SQLite FTS5

Use SQLite FTS5 or an equivalent SQLite-native full-text mechanism.

The search index SHOULD contain:

- `contentItemId`
- `artifactId`
- `ownerId`
- `title`
- `description`
- `author`
- `sourceName`
- `bodyText`
- `tags`

All search queries MUST include owner scoping.

## Index lifecycle

1. Artifact becomes valid.
2. Backend extracts indexable text.
3. Backend inserts or updates the FTS row.
4. Content item becomes searchable.
5. If indexing fails, artifact remains readable but capture status should expose a warning.

## Reindexing

Provide an administrative or CLI operation to:

- Rebuild all indexes.
- Rebuild one content item.
- Report missing or corrupt index entries.

## Search behavior

- Case-insensitive.
- Supports phrase search if FTS configuration allows it.
- Returns relevance ranking.
- Highlights MAY be added later.
- Search failures MUST not expose SQL syntax or internal errors.
