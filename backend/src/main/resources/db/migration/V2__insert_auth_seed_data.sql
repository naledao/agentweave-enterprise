INSERT INTO roles (id, code, name, description) VALUES
('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrator', 'Full platform administrator'),
('00000000-0000-0000-0000-000000000002', 'OPERATOR', 'Operator', 'Operations user with tool access'),
('00000000-0000-0000-0000-000000000003', 'USER', 'User', 'Standard business user');

INSERT INTO permissions (id, code, name, type, description) VALUES
('10000000-0000-0000-0000-000000000001', 'auth:user:read', 'Read users', 'API', 'Read user profiles and assignments'),
('10000000-0000-0000-0000-000000000002', 'auth:user:write', 'Write users', 'API', 'Create and update users'),
('10000000-0000-0000-0000-000000000003', 'auth:role:read', 'Read roles', 'API', 'Read roles and permissions'),
('10000000-0000-0000-0000-000000000004', 'auth:role:write', 'Write roles', 'API', 'Create and update roles'),
('10000000-0000-0000-0000-000000000005', 'tool:ticket:query', 'Query tickets', 'TOOL', 'Invoke the ticket query tool'),
('10000000-0000-0000-0000-000000000006', 'tool:log:search', 'Search logs', 'TOOL', 'Invoke the log search tool'),
('10000000-0000-0000-0000-000000000007', 'tool:api-status:query', 'Query API status', 'TOOL', 'Invoke the API status tool');

INSERT INTO role_permissions (role_id, permission_id)
SELECT CAST('00000000-0000-0000-0000-000000000001' AS UUID), id FROM permissions;

INSERT INTO role_permissions (role_id, permission_id) VALUES
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000006'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000007');

INSERT INTO role_permissions (role_id, permission_id) VALUES
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000005'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000007');
