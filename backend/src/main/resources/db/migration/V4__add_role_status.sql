ALTER TABLE roles
    ADD COLUMN status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_roles_status ON roles(status);
