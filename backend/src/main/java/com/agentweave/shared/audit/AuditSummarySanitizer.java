package com.agentweave.shared.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AuditSummarySanitizer {

    private static final String MASK = "******";
    private static final int DEFAULT_MAX_LENGTH = 1000;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "token",
            "secret",
            "authorization",
            "apikey",
            "api_key",
            "accesskey",
            "access_key",
            "phone",
            "idcard",
            "id_card");
    private static final Pattern SECRET_TEXT_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|authorization|api[-_ ]?key|access[-_ ]?key)\\s*[:=]\\s*[^,;\\s}\"]+");
    private static final Pattern PII_TEXT_PATTERN = Pattern.compile(
            "(?i)(phone|id[-_ ]?card)\\s*[:=]\\s*[^,;\\s}\"]+");

    private final ObjectMapper objectMapper;

    public AuditSummarySanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String summarize(Object value) {
        return summarize(value, DEFAULT_MAX_LENGTH);
    }

    public String summarize(Object value, int maxLength) {
        if (value == null) {
            return null;
        }
        String serialized = serialize(value);
        return truncate(maskText(serialized), maxLength);
    }

    public String sanitizeText(String value) {
        return sanitizeText(value, DEFAULT_MAX_LENGTH);
    }

    public String sanitizeText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return truncate(maskText(value), maxLength);
    }

    private String serialize(Object value) {
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            JsonNode tree = objectMapper.valueToTree(value);
            maskNode(tree);
            return objectMapper.writeValueAsString(tree);
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof ObjectNode objectNode) {
            objectNode.fields().forEachRemaining(entry -> {
                if (isSensitiveKey(entry.getKey())) {
                    objectNode.put(entry.getKey(), MASK);
                } else {
                    maskNode(entry.getValue());
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskNode);
        }
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.replace("-", "_").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized);
    }

    private String maskText(String value) {
        String secretMasked = SECRET_TEXT_PATTERN.matcher(value).replaceAll("$1=" + MASK);
        return PII_TEXT_PATTERN.matcher(secretMasked).replaceAll("$1=" + MASK);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength));
    }
}
