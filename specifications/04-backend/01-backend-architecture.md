# Backend Architecture

## Stack

- Spring Boot
- Kotlin
- SQLite
- JPA/Hibernate 
- Flyway for migrations
- SQLite FTS5 for full-text search
- Filesystem-backed artifact storage

## Layering

```text
HTTP/API
  → Application services
    → Domain model and policies
      → Repositories
        → SQLite / artifact storage
```

## Modules

Suggested package boundaries:

```text
auth
content
capture
artifact
reader
search
tag
user
storage
common
```

The exact package layout may differ, but modules should follow business boundaries.

## Application services

Suggested services:

- `CaptureService`
- `CaptureValidationService`
- `ArtifactStorageService`
- `ContentItemService`
- `ReadingStateService`
- `SearchService`
- `TagService`
- `UserService`

Controllers MUST remain thin. Business rules belong in services/domain policies.

## Transaction boundaries

- Database metadata changes SHOULD be transactional.
- Artifact file writes MUST use a temporary staging area.
- The backend MUST not mark an artifact ready until all required files are durably stored and validated.
- If database commit fails after file storage, a cleanup job SHOULD remove orphaned staged files.
- If file storage fails, the database transaction MUST not expose a ready artifact.

## Configuration

Configuration MUST support:

- Server port.
- Public base URL.
- SQLite path.
- Artifact storage path.
- Maximum package size.
- Maximum PDF size.
- Maximum asset count.
- Maximum individual asset size.
- Authentication settings.
- CORS or extension origin settings.
- Logging level.
- Backup settings.
