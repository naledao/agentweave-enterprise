package com.agentweave.knowledge.messaging.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.document-pipeline.rabbitmq")
public record RabbitMqDocumentProperties(
        boolean enabled,
        @NotBlank String exchange,
        @NotBlank String retryExchange,
        @NotBlank String deadLetterExchange,
        @NotBlank String queuePrefix) {

    public String parseQueue() {
        return queuePrefix + ".parse.queue";
    }

    public String chunkQueue() {
        return queuePrefix + ".chunk.queue";
    }

    public String vectorIndexQueue() {
        return queuePrefix + ".vector-index.queue";
    }

    public String graphRagIndexQueue() {
        return queuePrefix + ".graphrag-index.queue";
    }

    public String reindexQueue() {
        return queuePrefix + ".reindex.queue";
    }

    public String parseDlq() {
        return queuePrefix + ".parse.dlq";
    }

    public String chunkDlq() {
        return queuePrefix + ".chunk.dlq";
    }

    public String vectorIndexDlq() {
        return queuePrefix + ".vector-index.dlq";
    }

    public String graphRagIndexDlq() {
        return queuePrefix + ".graphrag-index.dlq";
    }

    public String reindexDlq() {
        return queuePrefix + ".reindex.dlq";
    }
}
