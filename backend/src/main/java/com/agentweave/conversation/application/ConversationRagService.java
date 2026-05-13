package com.agentweave.conversation.application;

import com.agentweave.graphrag.application.GraphRagRetrievalService;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.agentweave.springai.rag.VectorRetrievalService;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import com.agentweave.shared.security.CurrentUserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConversationRagService {

    private static final Logger log = LoggerFactory.getLogger(ConversationRagService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.0;
    private static final String SEARCH_PERMISSION = "knowledge:rag:search";

    private final VectorRetrievalService vectorRetrievalService;
    private final GraphRagRetrievalService graphRagRetrievalService;
    private final RagContextMerger ragContextMerger;
    private final CurrentUserService currentUserService;

    public ConversationRagService(
            VectorRetrievalService vectorRetrievalService,
            GraphRagRetrievalService graphRagRetrievalService,
            RagContextMerger ragContextMerger,
            CurrentUserService currentUserService) {
        this.vectorRetrievalService = vectorRetrievalService;
        this.graphRagRetrievalService = graphRagRetrievalService;
        this.ragContextMerger = ragContextMerger;
        this.currentUserService = currentUserService;
    }

    public RagPromptContext retrieve(ConversationPrompt prompt) {
        if (!currentUserService.hasPermission(SEARCH_PERMISSION)) {
            return ragContextMerger.merge(List.of(), List.of());
        }
        try {
            VectorRagSearchResponse vectorResponse = vectorRetrievalService.search(new VectorRagSearchRequest(
                    prompt.latestUserMessage(),
                    null,
                    null,
                    null,
                    null,
                    DEFAULT_TOP_K,
                    DEFAULT_SIMILARITY_THRESHOLD));
            GraphRagRetrievalResponse graphResponse = graphRagRetrievalService.retrieve(prompt, vectorResponse);
            return ragContextMerger.merge(vectorResponse.citations(), graphResponse.graphPaths());
        } catch (RuntimeException ex) {
            log.warn(
                    "Conversation RAG retrieval failed, falling back to empty context: conversationId={}",
                    prompt.conversationId(),
                    ex);
            return ragContextMerger.merge(List.of(), List.of());
        }
    }
}
