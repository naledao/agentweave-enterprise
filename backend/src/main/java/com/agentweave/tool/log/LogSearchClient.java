package com.agentweave.tool.log;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LogSearchClient {

    private final List<LogRecord> demoLogs = List.of(
            new LogRecord(
                    Instant.parse("2026-05-13T09:45:10Z"),
                    "knowledge-service",
                    "ERROR",
                    "Document indexing failed for docId=doc-1001 token=sk-demo-secret retryCount=3",
                    "trace-kg-1001"),
            new LogRecord(
                    Instant.parse("2026-05-13T09:43:02Z"),
                    "knowledge-service",
                    "WARN",
                    "Embedding provider latency exceeded threshold for document chunk chunk-7788",
                    "trace-kg-1002"),
            new LogRecord(
                    Instant.parse("2026-05-13T15:50:30Z"),
                    "chat-service",
                    "ERROR",
                    "SSE stream disconnected while invoking tool, userPhone=13812345678",
                    "trace-chat-2001"),
            new LogRecord(
                    Instant.parse("2026-05-13T15:48:11Z"),
                    "chat-service",
                    "ERROR",
                    "Tool execution timeout for ticket query password=plain-demo-pass",
                    "trace-chat-2002"),
            new LogRecord(
                    Instant.parse("2026-05-12T11:27:44Z"),
                    "rag-service",
                    "ERROR",
                    "Vector citation refresh failed for documentId=doc-9001 idCard=110101199003078888",
                    "trace-rag-3001"),
            new LogRecord(
                    Instant.parse("2026-05-12T11:21:07Z"),
                    "rag-service",
                    "INFO",
                    "Vector search completed for query='stale citation' hitCount=8",
                    "trace-rag-3002"));

    public List<LogRecord> search(LogQueryRequest request, int limit) {
        if ("log-service".equals(request.normalizedServiceName())
                && "simulate-downstream-error".equalsIgnoreCase(request.normalizedKeyword())) {
            throw new LogSearchException("log search service unavailable");
        }
        String serviceName = request.normalizedServiceName();
        String keyword = request.normalizedKeyword().toLowerCase(Locale.ROOT);
        Instant from = request.timeRange().from();
        Instant to = request.timeRange().to();

        return demoLogs.stream()
                .filter(item -> item.serviceName().equalsIgnoreCase(serviceName))
                .filter(item -> containsKeyword(item, keyword))
                .filter(item -> !item.timestamp().isBefore(from) && !item.timestamp().isAfter(to))
                .sorted(Comparator.comparing(LogRecord::timestamp).reversed())
                .limit(limit)
                .toList();
    }

    public long count(LogQueryRequest request) {
        String serviceName = request.normalizedServiceName();
        String keyword = request.normalizedKeyword().toLowerCase(Locale.ROOT);
        Instant from = request.timeRange().from();
        Instant to = request.timeRange().to();

        return demoLogs.stream()
                .filter(item -> item.serviceName().equalsIgnoreCase(serviceName))
                .filter(item -> containsKeyword(item, keyword))
                .filter(item -> !item.timestamp().isBefore(from) && !item.timestamp().isAfter(to))
                .count();
    }

    private boolean containsKeyword(LogRecord item, String keyword) {
        return item.message().toLowerCase(Locale.ROOT).contains(keyword)
                || item.level().toLowerCase(Locale.ROOT).contains(keyword)
                || item.traceId().toLowerCase(Locale.ROOT).contains(keyword);
    }
}
