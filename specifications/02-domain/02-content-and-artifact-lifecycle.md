# Content and Artifact Lifecycle

## Content item lifecycle

```text
Created
  → Active
  → Archived or Deleted
```

The MVP may omit a separate archived state and use deletion only, but the domain should not prevent adding archive behavior later.

## Capture lifecycle

```text
QUEUED
  → UPLOADING
  → PROCESSING
  → READY
```

Failure can occur from any active state:

```text
QUEUED / UPLOADING / PROCESSING → FAILED
FAILED → QUEUED
```

Cancellation is allowed before the artifact becomes ready.

## Immutability rules

- An artifact's bytes, manifest, checksum, and normalized reader content MUST be immutable.
- User-facing metadata MAY change.
- Reading state MAY change.
- Tags and favorite state MAY change.
- A refresh MUST create a new artifact version.
- The current artifact pointer MAY change to a newer immutable version.

## Duplicate handling

The backend SHOULD calculate a checksum over the canonical artifact package or artifact bytes.

Duplicate policy:

1. Detect an exact duplicate for the same owner.
2. Avoid storing duplicate binary assets when practical.
3. Either attach the existing artifact to the existing content item or report a duplicate to the extension.
4. Do not silently overwrite an existing artifact.

The initial implementation MAY use URL plus checksum as a practical duplicate heuristic.
