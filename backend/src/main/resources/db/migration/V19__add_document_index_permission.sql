INSERT INTO permissions (id, code, name, type, description)
VALUES (
    '10000000-0000-0000-0000-000000000010',
    'knowledge:document:index',
    'Index knowledge documents',
    'API',
    'Generate embeddings and write document chunks into the vector store'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code = 'knowledge:document:index'
ON CONFLICT DO NOTHING;
