package com.agentweave.observability.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VectorStoreHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;
    private final String tableName;
    private final int dimensions;

    public VectorStoreHealthIndicator(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.ai.vectorstore.pgvector.schema-name:public}") String schemaName,
            @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:0}") int dimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.dimensions = dimensions;
    }

    @Override
    public Health health() {
        try {
            Boolean pgvectorInstalled = jdbcTemplate.queryForObject(
                    "select exists(select 1 from pg_extension where extname = 'vector')",
                    Boolean.class);
            Boolean tableExists = jdbcTemplate.queryForObject(
                    """
                            select exists(
                                select 1
                                from information_schema.tables
                                where table_schema = ? and table_name = ?
                            )
                            """,
                    Boolean.class,
                    schemaName,
                    tableName);
            Health.Builder builder = Boolean.TRUE.equals(pgvectorInstalled) && Boolean.TRUE.equals(tableExists)
                    ? Health.up()
                    : Health.down();
            return builder
                    .withDetail("schema", schemaName)
                    .withDetail("table", tableName)
                    .withDetail("dimensions", dimensions)
                    .withDetail("pgvectorInstalled", Boolean.TRUE.equals(pgvectorInstalled))
                    .withDetail("tableExists", Boolean.TRUE.equals(tableExists))
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("schema", schemaName)
                    .withDetail("table", tableName)
                    .withDetail("dimensions", dimensions)
                    .build();
        }
    }
}
