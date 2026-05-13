package com.agentweave.knowledge.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class RabbitMqDocumentTopologyConfigTest {

    @Test
    void declaresDocumentPipelineTopology() {
        RabbitMqDocumentProperties properties = new RabbitMqDocumentProperties(
                true,
                "agentweave.document.exchange",
                "agentweave.document.retry.exchange",
                "agentweave.document.dlx",
                "agentweave.document");

        Declarables declarables = new RabbitMqDocumentTopologyConfig().documentPipelineDeclarables(properties);
        Collection<Declarable> items = declarables.getDeclarables();

        assertThat(items).filteredOn(TopicExchange.class::isInstance).hasSize(3);
        assertThat(items).filteredOn(Queue.class::isInstance).hasSize(10);
        assertThat(items)
                .filteredOn(Queue.class::isInstance)
                .map(item -> ((Queue) item).getName())
                .contains(
                        "agentweave.document.parse.queue",
                        "agentweave.document.chunk.queue",
                        "agentweave.document.vector-index.queue",
                        "agentweave.document.graphrag-index.queue",
                        "agentweave.document.reindex.queue",
                        "agentweave.document.parse.dlq");
    }
}
