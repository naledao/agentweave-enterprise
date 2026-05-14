UPDATE tool_definitions
SET input_schema = '{"type":"object","properties":{"endpoint":{"type":"string","description":"已登记的接口路径、接口编码或内部服务端点"}},"required":["endpoint"]}',
    output_schema = '{"type":"object","properties":{"endpoint":{"type":"string"},"httpStatus":{"type":"integer"},"averageLatencyMs":{"type":"integer"},"failureRate":{"type":"number"},"checkedAt":{"type":"string","format":"date-time"}}}',
    updated_at = now()
WHERE code = 'endpoint.status';
