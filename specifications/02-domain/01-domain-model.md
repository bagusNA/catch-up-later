# Domain Model

## Entity overview

```text
User
 └── ContentItem
      ├── Source
      ├── ArtifactVersion
      │    ├── ArtifactAsset
      │    └── SearchDocument
      ├── Tag
      └── ReadingState
```

## User

Represents an authenticated account.

Required fields:

- `id`
- `username` or login identifier
- `passwordHash` or external-auth identity
- `createdAt`
- `updatedAt`
- `status`

The schema MUST include ownership on all user-private records even if the initial deployment has one user.

## ContentItem

Represents the stable library entry.

Suggested fields:

- `id`
- `ownerId`
- `title`
- `description`
- `sourceUrl`
- `canonicalUrl`
- `sourceName`
- `contentType`
- `status`
- `isFavorite`
- `createdAt`
- `updatedAt`
- `lastReadAt`
- `deletedAt`

`ContentItem` metadata is mutable. It may be renamed, tagged, favorited, or marked read without changing the artifact.

## ArtifactVersion

Represents immutable captured content.

Suggested fields:

- `id`
- `contentItemId`
- `versionNumber`
- `artifactType`
- `storageKey`
- `manifestStorageKey`
- `checksum`
- `byteSize`
- `capturedAt`
- `adapterId`
- `adapterVersion`
- `packageSchemaVersion`
- `validationStatus`
- `isCurrent`
- `createdAt`

Artifact bytes and normalized content MUST NOT be modified after successful storage. A new capture creates a new artifact version.

## ArtifactAsset

Represents an asset belonging to one artifact.

Suggested fields:

- `id`
- `artifactVersionId`
- `assetKey`
- `mimeType`
- `byteSize`
- `checksum`
- `storageKey`
- `originalUrl`
- `altText`
- `width`
- `height`

Assets MUST be scoped to an artifact and MUST NOT be addressed by arbitrary filesystem paths.

## Tag

Suggested fields:

- `id`
- `ownerId`
- `name`
- `normalizedName`
- `createdAt`

A unique constraint SHOULD exist on `(ownerId, normalizedName)`.

## ReadingState

Suggested fields:

- `id`
- `ownerId`
- `contentItemId`
- `status`
- `progressPercent`
- `position`
- `lastReadAt`
- `updatedAt`

For articles, `position` may be a normalized character offset or anchor. For PDFs, it should include page number and optionally scroll offset.

## Enumerations

### Content type

- `ARTICLE`
- `PDF`

### Artifact type

- `ARTICLE_READER`
- `PDF_DOCUMENT`

### Reading status

- `UNREAD`
- `IN_PROGRESS`
- `READ`

### Capture status

- `QUEUED`
- `UPLOADING`
- `PROCESSING`
- `READY`
- `FAILED`
- `CANCELLED`

### Validation status

- `PENDING`
- `VALID`
- `VALID_WITH_WARNINGS`
- `INVALID`
