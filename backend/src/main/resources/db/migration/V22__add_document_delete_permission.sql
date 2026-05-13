INSERT INTO permissions (id, code, name, type, description)
VALUES (
    '10000000-0000-0000-0000-000000000012',
    'knowledge:document:delete',
    'Delete knowledge documents',
    'API',
    'Delete uploaded documents and clean up related storage and index data'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code = 'knowledge:document:delete'
ON CONFLICT DO NOTHING;
