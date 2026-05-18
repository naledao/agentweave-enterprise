package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.application.KnowledgeStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.unit.DataSize;

class HealthIndicatorTest {

    @Test
    void modelProviderHealthIsDownWhenRequiredConfigurationIsMissing() {
        ModelProviderHealthIndicator indicator = new ModelProviderHealthIndicator(
                "mimo-v2.5",
                "qwen3-embedding:0.6b",
                "https://api.openai.com/v1",
                "http://14.103.202.40:11434",
                "");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("apiKeyConfigured", false);
    }

    @Test
    void modelProviderHealthIsUpWhenRequiredConfigurationExists() {
        ModelProviderHealthIndicator indicator = new ModelProviderHealthIndicator(
                "mimo-v2.5",
                "qwen3-embedding:0.6b",
                "https://api.openai.com/v1",
                "http://14.103.202.40:11434",
                "sk-test");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("chatModel", "mimo-v2.5");
    }

    @Test
    void minioHealthIsUpWhenBucketExists() throws Exception {
        MinioClient minioClient = org.mockito.Mockito.mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        MinioHealthIndicator indicator = new MinioHealthIndicator(minioClient, storageProperties(false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bucketExists", true);
    }

    @Test
    void minioHealthIsDownWhenBucketIsMissingAndAutoCreateDisabled() throws Exception {
        MinioClient minioClient = org.mockito.Mockito.mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        MinioHealthIndicator indicator = new MinioHealthIndicator(minioClient, storageProperties(false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("bucketExists", false);
    }

    private KnowledgeStorageProperties storageProperties(boolean createBucketIfMissing) {
        return new KnowledgeStorageProperties(
                "http://14.103.202.40:9000",
                "agentweave",
                "secret",
                "agentweave-documents",
                createBucketIfMissing,
                DataSize.ofMegabytes(50),
                java.util.List.of("text/plain"),
                java.util.List.of("txt"));
    }
}
