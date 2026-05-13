CREATE TABLE documents (
    id UUID PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_bucket VARCHAR(120) NOT NULL,
    storage_object_key VARCHAR(500) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    status VARCHAR(40) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    source VARCHAR(160) NOT NULL,
    business_domain VARCHAR(120) NOT NULL,
    document_type VARCHAR(80) NOT NULL,
    permission_level VARCHAR(80) NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE,
    effective_to TIMESTAMP WITH TIME ZONE,
    tags TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_documents_storage_location UNIQUE (storage_bucket, storage_object_key),
    CONSTRAINT chk_documents_file_size_positive CHECK (file_size > 0)
);

CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at);
CREATE INDEX idx_documents_business_domain ON documents(business_domain);
CREATE INDEX idx_documents_document_type ON documents(document_type);
CREATE INDEX idx_documents_permission_level ON documents(permission_level);

INSERT INTO permissions (id, code, name, type, description)
VALUES (
    '10000000-0000-0000-0000-000000000008',
    'knowledge:document:upload',
    'Upload knowledge documents',
    'API',
    'Upload source documents into the knowledge base'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code = 'knowledge:document:upload'
ON CONFLICT DO NOTHING;
