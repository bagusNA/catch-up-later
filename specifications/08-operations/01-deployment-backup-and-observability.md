# Deployment, Backup, and Observability

## Deployment

Provide a Docker image and a Docker Compose example.

Persistent volumes:

- SQLite database.
- Artifact storage.
- Backup directory, if enabled.

The application MUST start with an empty data directory and perform migrations safely.

## Configuration

Document:

- `APP_BASE_URL`
- `APP_PORT`
- `APP_DATABASE_PATH`
- `APP_ARTIFACTS_PATH`
- `APP_MAX_CAPTURE_BYTES`
- `APP_MAX_PDF_BYTES`
- `APP_MAX_ASSET_BYTES`
- `APP_MAX_ASSET_COUNT`
- Authentication configuration.
- Logging configuration.

Names may be changed, but all settings must be documented.

## Health

Provide:

- Liveness endpoint.
- Readiness endpoint.
- Database connectivity check.
- Artifact storage accessibility check.

## Logging

Use structured logs with:

- Timestamp.
- Level.
- Request ID.
- User ID where safe.
- Capture ID.
- Artifact ID.
- Error code.

Never log passwords, tokens, full HTML, or sensitive document contents.

## Metrics

MVP may use basic logs, but the architecture SHOULD allow metrics for:

- Capture success/failure counts.
- Capture duration.
- Artifact storage usage.
- Search duration.
- Reader errors.
- Queue length.

## Backup

Document a consistent backup procedure that includes both SQLite and artifact files.

The application SHOULD provide:

- Manual backup command.
- Restore command or documented restore process.
- Backup integrity verification.
- Warning when database and artifact backup timestamps differ.

## Upgrades

- Database migrations MUST be versioned.
- Backward-incompatible capture package changes require schema versioning.
- Upgrade documentation MUST mention backup before migration.
