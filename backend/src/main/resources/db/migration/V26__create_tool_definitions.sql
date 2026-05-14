INSERT INTO permissions (id, code, name, type, description) VALUES
('10000000-0000-0000-0000-000000000005', 'tool:ticket:query', 'Query tickets', 'TOOL', 'Invoke the ticket query tool'),
('10000000-0000-0000-0000-000000000006', 'tool:log:search', 'Search logs', 'TOOL', 'Invoke the log search tool'),
('10000000-0000-0000-0000-000000000007', 'tool:api-status:query', 'Query API status', 'TOOL', 'Invoke the API status tool')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    type = EXCLUDED.type,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('tool:ticket:query', 'tool:log:search', 'tool:api-status:query')
WHERE r.code IN ('ADMIN', 'OPERATOR')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('tool:ticket:query', 'tool:api-status:query')
WHERE r.code = 'USER'
ON CONFLICT DO NOTHING;

CREATE TABLE tool_definitions (
    id UUID PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    permission_code VARCHAR(120) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    input_schema TEXT,
    output_schema TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tool_definitions_permission_code
        FOREIGN KEY (permission_code) REFERENCES permissions(code),
    CONSTRAINT ck_tool_definitions_risk_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX idx_tool_definitions_enabled ON tool_definitions(enabled);
CREATE INDEX idx_tool_definitions_permission_code ON tool_definitions(permission_code);

INSERT INTO tool_definitions (
    id,
    code,
    name,
    description,
    permission_code,
    risk_level,
    enabled,
    input_schema,
    output_schema
) VALUES
(
    '20000000-0000-0000-0000-000000000001',
    'ticket.query',
    '工单查询',
    '按工单号查询工单摘要、状态和处理信息。',
    'tool:ticket:query',
    'LOW',
    true,
    '{"type":"object","properties":{"ticketNo":{"type":"string","description":"工单号"}},"required":["ticketNo"]}',
    '{"type":"object","properties":{"ticketNo":{"type":"string"},"title":{"type":"string"},"state":{"type":"string"}}}'
),
(
    '20000000-0000-0000-0000-000000000002',
    'log.search',
    '日志检索',
    '按关键词检索业务日志片段，用于排障辅助分析。',
    'tool:log:search',
    'MEDIUM',
    true,
    '{"type":"object","properties":{"keyword":{"type":"string","description":"日志关键词"}},"required":["keyword"]}',
    '{"type":"object","properties":{"keyword":{"type":"string"},"matches":{"type":"integer"}}}'
),
(
    '20000000-0000-0000-0000-000000000003',
    'endpoint.status',
    '接口状态查询',
    '查询内部服务或接口的可用状态和延迟摘要。',
    'tool:api-status:query',
    'LOW',
    true,
    '{"type":"object","properties":{"service":{"type":"string","description":"服务或接口标识"}},"required":["service"]}',
    '{"type":"object","properties":{"service":{"type":"string"},"status":{"type":"string"},"latencyMs":{"type":"integer"}}}'
);
