ALTER TABLE tool_definitions
    ADD COLUMN tool_type VARCHAR(40);

UPDATE tool_definitions
SET tool_type = CASE code
    WHEN 'ticket.query' THEN 'BUSINESS_QUERY'
    WHEN 'log.search' THEN 'LOG_SEARCH'
    WHEN 'endpoint.status' THEN 'ENDPOINT_STATUS'
    ELSE 'UNKNOWN'
END;

ALTER TABLE tool_definitions
    ALTER COLUMN tool_type SET NOT NULL,
    ADD CONSTRAINT ck_tool_definitions_tool_type
        CHECK (tool_type IN (
            'BUSINESS_QUERY',
            'LOG_SEARCH',
            'DATABASE_READ',
            'ENDPOINT_STATUS',
            'NOTIFICATION',
            'MCP_RESOURCE',
            'SCRIPT',
            'UNKNOWN'
        ));

CREATE INDEX idx_tool_definitions_tool_type
    ON tool_definitions(tool_type);

ALTER TABLE tool_invocations
    ADD COLUMN tool_type VARCHAR(40);

UPDATE tool_invocations inv
SET tool_type = def.tool_type
FROM tool_definitions def
WHERE inv.tool_code = def.code;

UPDATE tool_invocations
SET tool_type = 'UNKNOWN'
WHERE tool_type IS NULL;

ALTER TABLE tool_invocations
    ALTER COLUMN tool_type SET NOT NULL,
    ADD CONSTRAINT ck_tool_invocations_tool_type
        CHECK (tool_type IN (
            'BUSINESS_QUERY',
            'LOG_SEARCH',
            'DATABASE_READ',
            'ENDPOINT_STATUS',
            'NOTIFICATION',
            'MCP_RESOURCE',
            'SCRIPT',
            'UNKNOWN'
        ));

CREATE INDEX idx_tool_invocations_tool_type_created_at
    ON tool_invocations(tool_type, created_at DESC);
