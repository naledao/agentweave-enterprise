INSERT INTO permissions (id, code, name, type, description)
VALUES (
    '10000000-0000-0000-0000-000000000011',
    'knowledge:rag:search',
    'Search RAG knowledge base',
    'API',
    'Run vector RAG retrieval over indexed knowledge chunks'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
CROSS JOIN permissions
WHERE roles.code = 'ADMIN'
  AND permissions.code = 'knowledge:rag:search'
ON CONFLICT DO NOTHING;
