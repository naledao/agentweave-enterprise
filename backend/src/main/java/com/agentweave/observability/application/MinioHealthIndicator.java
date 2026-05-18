package com.agentweave.observability.application;

import com.agentweave.knowledge.application.KnowledgeStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final KnowledgeStorageProperties properties;

    public MinioHealthIndicator(MinioClient minioClient, KnowledgeStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.bucket())
                    .build());
            Health.Builder builder = bucketExists || properties.createBucketIfMissing()
                    ? Health.up()
                    : Health.down();
            return builder
                    .withDetail("endpoint", properties.endpoint())
                    .withDetail("bucket", properties.bucket())
                    .withDetail("bucketExists", bucketExists)
                    .withDetail("createBucketIfMissing", properties.createBucketIfMissing())
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("endpoint", properties.endpoint())
                    .withDetail("bucket", properties.bucket())
                    .build();
        }
    }
}
