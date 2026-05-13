ALTER TABLE documents
    ADD COLUMN parsed_text TEXT;

INSERT INTO permissions (id, code, name, type, description)
VALUES (
    '10000000-0000-0000-0000-000000000009',
    'knowledge:document:parse',
    'Parse knowledge documents',
    'API',
    'Parse uploaded source documents into raw text'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code = 'knowledge:document:parse'
ON CONFLICT DO NOTHING;
