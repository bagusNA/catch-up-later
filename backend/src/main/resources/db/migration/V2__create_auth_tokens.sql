-- Bearer access/refresh tokens for the browser extension.
--
-- Tokens are opaque random strings; only their SHA-256 hash is stored, so a
-- database leak does not expose usable credentials. Access and refresh tokens
-- share a family id. Refresh tokens rotate on every use and replay of a rotated
-- token revokes the whole family.

CREATE TABLE auth_tokens (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    token_hash   VARCHAR(64) NOT NULL,
    token_type   VARCHAR(16) NOT NULL,
    family_id    VARCHAR(36) NOT NULL,
    expires_at   TIMESTAMP NOT NULL,
    revoked      BOOLEAN NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    CONSTRAINT uk_auth_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_tokens_user_id ON auth_tokens (user_id);
CREATE INDEX idx_auth_tokens_family_id ON auth_tokens (family_id);
CREATE INDEX idx_auth_tokens_expires_at ON auth_tokens (expires_at);
