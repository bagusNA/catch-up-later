-- Authentication schema for the catch-up-later application.
--
-- Notes:
--  * SQLite is the production database for this self-hosted application.
--  * Email uniqueness is enforced case-insensitively at the database level via
--    COLLATE NOCASE on the column, so the UNIQUE constraint is case-insensitive.
--    The application additionally normalises emails to lower case.
--  * Roles are database-backed and referenced through the user_roles join table.
--  * Foreign keys must be enabled per connection (PRAGMA foreign_keys=ON); the
--    datasource is configured with a Hikari connection-init-sql for this.
--  * Column type names are chosen to match Hibernate's SQLite type mappings so
--    that `ddl-auto=validate` can verify the schema at startup.

CREATE TABLE roles (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         VARCHAR(320) NOT NULL COLLATE NOCASE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    enabled       BOOLEAN NOT NULL DEFAULT 1,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
