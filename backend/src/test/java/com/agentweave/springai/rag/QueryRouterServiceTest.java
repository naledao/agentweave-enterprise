package com.agentweave.springai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.security.CurrentUser;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QueryRouterServiceTest {

    private final QueryRouterService queryRouterService = new QueryRouterService();

    @Test
    void routesFactualQuestionToVectorOnlyByDefault() {
        RagRoutingDecision decision = queryRouterService.route(new RagRoutingRequest(
                "\u8ba2\u5355\u63a5\u53e3\u8d85\u65f6\u600e\u4e48\u6392\u67e5",
                UUID.randomUUID(),
                "order",
                "RUNBOOK",
                "INTERNAL",
                null,
                currentUser()));

        assertThat(decision.retrievalMode()).isEqualTo(RagRetrievalMode.VECTOR_ONLY);
        assertThat(decision.routingReason()).contains("default vector");
        assertThat(decision.retrievalPlan().shouldSearchVector()).isTrue();
        assertThat(decision.retrievalPlan().shouldSearchGraph()).isFalse();
        assertThat(decision.retrievalPlan().vectorTopK()).isEqualTo(5);
        assertThat(decision.retrievalPlan().similarityThreshold()).isEqualTo(0.0);
        assertThat(decision.retrievalPlan().graphMaxDepth()).isZero();
        assertThat(decision.retrievalPlan().metadataFilter())
                .containsEntry("businessDomain", "order")
                .containsEntry("documentType", "RUNBOOK")
                .containsEntry("permissionLevel", "INTERNAL");
        assertThat(decision.retrievalPlan().graphFilter())
                .containsEntry("businessDomain", "order")
                .containsEntry("permissionLevel", "INTERNAL")
                .doesNotContainKey("documentType");
    }

    @Test
    void routesDependencyAndImpactQuestionsToHybrid() {
        RagRoutingDecision dependencyDecision = queryRouterService.route(new RagRoutingRequest(
                "\u652f\u4ed8\u5931\u8d25\u4f1a\u5f71\u54cd\u54ea\u4e9b\u4e0a\u4e0b\u6e38\u670d\u52a1",
                UUID.randomUUID(),
                "payment",
                null,
                "CONFIDENTIAL",
                null,
                currentUser()));

        assertThat(dependencyDecision.retrievalMode()).isEqualTo(RagRetrievalMode.HYBRID);
        assertThat(dependencyDecision.routingReason()).contains("relationship");
        assertThat(dependencyDecision.retrievalPlan().shouldSearchVector()).isTrue();
        assertThat(dependencyDecision.retrievalPlan().shouldSearchGraph()).isTrue();
        assertThat(dependencyDecision.retrievalPlan().vectorTopK()).isEqualTo(5);
        assertThat(dependencyDecision.retrievalPlan().graphMaxDepth()).isEqualTo(2);

        RagRoutingDecision rootCauseDecision = queryRouterService.route(new RagRoutingRequest(
                "Why does order service depend on payment gateway?",
                UUID.randomUUID(),
                "order",
                "FAQ",
                "INTERNAL",
                null,
                currentUser()));

        assertThat(rootCauseDecision.retrievalMode()).isEqualTo(RagRetrievalMode.HYBRID);
        assertThat(rootCauseDecision.retrievalPlan().metadataFilter())
                .containsEntry("businessDomain", "order")
                .containsEntry("documentType", "FAQ")
                .containsEntry("permissionLevel", "INTERNAL");
    }

    @Test
    void routesExplicitRelationshipPathQuestionsToGraphOnly() {
        RagRoutingDecision decision = queryRouterService.route(new RagRoutingRequest(
                "\u67e5\u770b\u8ba2\u5355\u670d\u52a1\u5230\u652f\u4ed8\u670d\u52a1\u7684\u5173\u7cfb\u8def\u5f84",
                UUID.randomUUID(),
                "order",
                "ARCHITECTURE",
                "INTERNAL",
                null,
                currentUser()));

        assertThat(decision.retrievalMode()).isEqualTo(RagRetrievalMode.GRAPH_ONLY);
        assertThat(decision.routingReason()).contains("graph path");
        assertThat(decision.retrievalPlan().shouldSearchVector()).isFalse();
        assertThat(decision.retrievalPlan().shouldSearchGraph()).isTrue();
        assertThat(decision.retrievalPlan().vectorTopK()).isZero();
        assertThat(decision.retrievalPlan().graphMaxDepth()).isEqualTo(2);
    }

    @Test
    void explicitRetrievalModeTakesPrecedenceOverQueryHints() {
        RagRoutingDecision decision = queryRouterService.route(new RagRoutingRequest(
                "\u4e0a\u4e0b\u6e38\u4f9d\u8d56\u548c\u5f71\u54cd\u8303\u56f4\u662f\u4ec0\u4e48",
                UUID.randomUUID(),
                "order",
                "RUNBOOK",
                "INTERNAL",
                RagRetrievalMode.VECTOR_ONLY,
                currentUser()));

        assertThat(decision.retrievalMode()).isEqualTo(RagRetrievalMode.VECTOR_ONLY);
        assertThat(decision.routingReason()).contains("explicit");
        assertThat(decision.retrievalPlan().shouldSearchGraph()).isFalse();
        assertThat(decision.retrievalPlan().graphMaxDepth()).isZero();
    }

    @Test
    void unsupportedRetrievalModeIsAValidationFailure() {
        assertThatThrownBy(() -> RagRetrievalMode.from("SEMANTIC_GRAPH"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Unsupported retrieval mode: SEMANTIC_GRAPH")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void blankExplicitRetrievalModeIsTreatedAsNotSpecified() {
        assertThat(RagRetrievalMode.from("  ")).isNull();
        assertThat(RagRetrievalMode.from(null)).isNull();
    }

    private CurrentUser currentUser() {
        return new CurrentUser(
                UUID.randomUUID(),
                "rag-user",
                "RAG User",
                Set.of("RAG_SEARCHER"),
                Set.of("knowledge:rag:search"));
    }
}
