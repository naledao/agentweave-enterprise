package com.agentweave.conversation.api;

import com.agentweave.conversation.application.ConversationStreamService;
import com.agentweave.conversation.dto.CancelMessageResponse;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationStreamController {

    private final ConversationStreamService conversationStreamService;

    public ConversationStreamController(ConversationStreamService conversationStreamService) {
        this.conversationStreamService = conversationStreamService;
    }

    @GetMapping(path = "/{conversationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> stream(@PathVariable UUID conversationId) {
        return conversationStreamService.stream(conversationId);
    }

    @PostMapping("/{conversationId}/messages/{messageId}/cancel")
    public CancelMessageResponse cancel(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        return conversationStreamService.cancel(conversationId, messageId);
    }
}
