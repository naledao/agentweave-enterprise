package com.agentweave.conversation.application;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {

    private final ChatMemory chatMemory;

    public ConversationMemoryService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public void syncBeforeModelCall(ConversationPrompt prompt) {
        String conversationId = prompt.conversationId().toString();
        chatMemory.clear(conversationId);
        chatMemory.add(conversationId, toMemoryMessages(prompt));
    }

    private List<Message> toMemoryMessages(ConversationPrompt prompt) {
        List<ConversationTurn> turns = prompt.turns();
        int latestUserIndex = latestUserTurnIndex(turns);
        List<Message> messages = new ArrayList<>();
        for (int index = 0; index < turns.size(); index++) {
            if (index == latestUserIndex) {
                continue;
            }
            messages.add(toMessage(turns.get(index)));
        }
        return messages;
    }

    private int latestUserTurnIndex(List<ConversationTurn> turns) {
        for (int index = turns.size() - 1; index >= 0; index--) {
            ConversationTurn turn = turns.get(index);
            if ("USER".equals(turn.role())) {
                return index;
            }
        }
        return -1;
    }

    private Message toMessage(ConversationTurn turn) {
        return switch (turn.role()) {
            case "ASSISTANT" -> new AssistantMessage(turn.content());
            case "SYSTEM" -> new SystemMessage(turn.content());
            default -> new UserMessage(turn.content());
        };
    }
}
