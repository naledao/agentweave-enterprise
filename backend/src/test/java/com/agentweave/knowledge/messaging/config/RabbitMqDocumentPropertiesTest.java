package com.agentweave.knowledge.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RabbitMqDocumentPropertiesTest {

    @Test
    void buildsQueueAndDlqNamesFromPrefix() {
        RabbitMqDocumentProperties properties = new RabbitMqDocumentProperties(
                true,
                "agentweave.document.exchange",
                "agentweave.document.retry.exchange",
                "agentweave.document.dlx",
                "agentweave.document");

        assertThat(properties.parseQueue()).isEqualTo("agentweave.document.parse.queue");
        assertThat(properties.chunkQueue()).isEqualTo("agentweave.document.chunk.queue");
        assertThat(properties.vectorIndexQueue()).isEqualTo("agentweave.document.vector-index.queue");
        assertThat(properties.graphRagIndexQueue()).isEqualTo("agentweave.document.graphrag-index.queue");
        assertThat(properties.reindexQueue()).isEqualTo("agentweave.document.reindex.queue");
        assertThat(properties.parseDlq()).isEqualTo("agentweave.document.parse.dlq");
        assertThat(properties.chunkDlq()).isEqualTo("agentweave.document.chunk.dlq");
        assertThat(properties.vectorIndexDlq()).isEqualTo("agentweave.document.vector-index.dlq");
        assertThat(properties.graphRagIndexDlq()).isEqualTo("agentweave.document.graphrag-index.dlq");
        assertThat(properties.reindexDlq()).isEqualTo("agentweave.document.reindex.dlq");
    }
}
