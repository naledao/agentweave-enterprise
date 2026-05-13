package com.agentweave.conversation.api;

import com.agentweave.conversation.application.ChatApplicationService;
import com.agentweave.conversation.application.ConversationService;
import com.agentweave.conversation.dto.ConversationDetailResponse;
import com.agentweave.conversation.dto.ConversationListResponse;
import com.agentweave.conversation.dto.ConversationMessageQueryRequest;
import com.agentweave.conversation.dto.ConversationQueryRequest;
import com.agentweave.conversation.dto.CreateConversationRequest;
import com.agentweave.conversation.dto.ConversationResponse;
import com.agentweave.conversation.dto.SendMessageRequest;
import com.agentweave.conversation.dto.SendMessageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@Validated
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatApplicationService chatApplicationService;

    public ConversationController(
            ConversationService conversationService,
            ChatApplicationService chatApplicationService) {
        this.conversationService = conversationService;
        this.chatApplicationService = chatApplicationService;
    }

    @PostMapping
    public ConversationResponse create(@Valid @RequestBody CreateConversationRequest request) {
        return conversationService.create(request);
    }

    @GetMapping
    public ConversationListResponse list(@Valid ConversationQueryRequest request) {
        return conversationService.list(request);
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse get(
            @PathVariable UUID conversationId,
            @Valid ConversationMessageQueryRequest request) {
        return conversationService.get(conversationId, request);
    }

    @PostMapping("/{conversationId}/messages")
    public SendMessageResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatApplicationService.sendMessage(conversationId, request);
    }
}
