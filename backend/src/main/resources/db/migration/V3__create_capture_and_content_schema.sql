-- Capture intake, immutable artifacts, and the library content items.
--
-- Notes:
--  * `content_items` is the mutable library entry. `artifact_versions` holds
--    immutable captured bytes referenced by a generated storage key.
--  * `capture_jobs` tracks each upload attempt and is unique per
--    (owner_id, idempotency_key) so retries never create duplicate content.
--  * Column type names follow the mapping conventions established in V1 so
--    that `ddl-auto=validate` succeeds against Hibernate.

CREATE TABLE content_items (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id      INTEGER NOT NULL,
    title         VARCHAR(500) NOT NULL,
    description   VARCHAR(2000),
    source_url    VARCHAR(2048) NOT NULL,
    canonical_url VARCHAR(2048),
    source_name   VARCHAR(255),
    content_type  VARCHAR(16) NOT NULL,
    status        VARCHAR(16) NOT NULL,
    is_favorite   BOOLEAN NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    last_read_at  TIMESTAMP,
    deleted_at    TIMESTAMP,
    CONSTRAINT fk_content_items_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_content_items_owner ON content_items (owner_id);
CREATE INDEX idx_content_items_owner_status ON content_items (owner_id, status);

CREATE TABLE artifact_versions (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    content_item_id        INTEGER NOT NULL,
    version_number         INTEGER NOT NULL,
    artifact_type          VARCHAR(32) NOT NULL,
    storage_key            VARCHAR(512) NOT NULL,
    manifest_storage_key   VARCHAR(512) NOT NULL,
    checksum               VARCHAR(64) NOT NULL,
    byte_size              BIGINT NOT NULL,
    reading_time_minutes   INTEGER,
    captured_at            TIMESTAMP NOT NULL,
    adapter_id             VARCHAR(100) NOT NULL,
    adapter_version        VARCHAR(50) NOT NULL,
    package_schema_version INTEGER NOT NULL,
    validation_status      VARCHAR(32) NOT NULL,
    is_current             BOOLEAN NOT NULL DEFAULT 1,
    created_at             TIMESTAMP NOT NULL,
    CONSTRAINT uk_artifact_versions_item_version UNIQUE (content_item_id, version_number),
    CONSTRAINT fk_artifact_versions_item FOREIGN KEY (content_item_id) REFERENCES content_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_artifact_versions_item ON artifact_versions (content_item_id);
CREATE INDEX idx_artifact_versions_current ON artifact_versions (content_item_id, is_current);

CREATE TABLE artifact_assets (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    artifact_version_id INTEGER NOT NULL,
    asset_key          VARCHAR(128) NOT NULL,
    mime_type          VARCHAR(100) NOT NULL,
    byte_size          BIGINT NOT NULL,
    checksum           VARCHAR(64) NOT NULL,
    storage_key        VARCHAR(512) NOT NULL,
    original_url       VARCHAR(2048),
    alt_text           VARCHAR(1000),
    width              INTEGER,
    height             INTEGER,
    CONSTRAINT uk_artifact_assets_key UNIQUE (artifact_version_id, asset_key),
    CONSTRAINT fk_artifact_assets_version FOREIGN KEY (artifact_version_id) REFERENCES artifact_versions (id) ON DELETE CASCADE
);

CREATE INDEX idx_artifact_assets_version ON artifact_assets (artifact_version_id);

CREATE TABLE capture_jobs (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id            INTEGER NOT NULL,
    public_id           VARCHAR(36) NOT NULL,
    idempotency_key     VARCHAR(128) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    artifact_type       VARCHAR(32),
    source_url          VARCHAR(2048),
    content_item_id     INTEGER,
    artifact_version_id INTEGER,
    warnings            VARCHAR(4000),
    error_code          VARCHAR(64),
    error_message       VARCHAR(1024),
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    CONSTRAINT uk_capture_jobs_public_id UNIQUE (public_id),
    CONSTRAINT uk_capture_jobs_idempotency UNIQUE (owner_id, idempotency_key),
    CONSTRAINT fk_capture_jobs_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_capture_jobs_owner ON capture_jobs (owner_id);
