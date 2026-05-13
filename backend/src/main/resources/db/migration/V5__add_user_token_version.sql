ALTER TABLE users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_users_token_version ON users(token_version);
