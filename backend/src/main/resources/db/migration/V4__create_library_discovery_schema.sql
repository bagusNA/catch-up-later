-- Library and discovery: tags, reading state.
--
-- Notes:
--  * `tags` are unique per owner case-insensitively via `normalized_name`.
--  * `content_item_tags` is an explicit join table so tag assignment is a
--    first-class entity and never relies on JPA-owned collection semantics.
--  * `reading_states` is separate from `content_items.status` (DEC-006).
--  * The FTS5 search index lives in its own sidecar SQLite database (see
--    `SearchDatabaseConfig`). Keeping the virtual table out of the main schema
--    avoids Hibernate schema extraction failing on FTS5's typeless columns.

CREATE TABLE tags (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id        INTEGER NOT NULL,
    name            VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    CONSTRAINT uk_tags_owner_normalized UNIQUE (owner_id, normalized_name),
    CONSTRAINT fk_tags_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_tags_owner ON tags (owner_id);

CREATE TABLE content_item_tags (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    content_item_id INTEGER NOT NULL,
    tag_id          INTEGER NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    CONSTRAINT uk_content_item_tags UNIQUE (content_item_id, tag_id),
    CONSTRAINT fk_content_item_tags_item FOREIGN KEY (content_item_id) REFERENCES content_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_content_item_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE INDEX idx_content_item_tags_tag ON content_item_tags (tag_id);

CREATE TABLE reading_states (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id         INTEGER NOT NULL,
    content_item_id  INTEGER NOT NULL,
    status           VARCHAR(16) NOT NULL,
    progress_percent DOUBLE,
    position_type    VARCHAR(32),
    position_value   BIGINT,
    last_read_at     TIMESTAMP,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,
    CONSTRAINT uk_reading_states_item UNIQUE (content_item_id),
    CONSTRAINT fk_reading_states_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_states_item FOREIGN KEY (content_item_id) REFERENCES content_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_reading_states_owner_status ON reading_states (owner_id, status);
