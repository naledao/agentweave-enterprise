INSERT INTO permissions (id, code, name, type, description)
VALUES (
    '10000000-0000-0000-0000-000000000020',
    'observability:read',
    'Read observability data',
    'API',
    'Read audit logs and observability metrics'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code = 'observability:read'
ON CONFLICT DO NOTHING;
