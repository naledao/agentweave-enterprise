package com.agentweave.tool.log;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogQueryService {

    static final Duration MAX_TIME_RANGE = Duration.ofHours(24);
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final LogSearchClient logSearchClient;
    private final LogMaskingService logMaskingService;

    public LogQueryService(LogSearchClient logSearchClient, LogMaskingService logMaskingService) {
        this.logSearchClient = logSearchClient;
        this.logMaskingService = logMaskingService;
    }

    @Transactional(readOnly = true)
    public LogQueryResult query(LogQueryRequest request) {
        validateTimeRange(request);
        int limit = Math.min(request.requestedLimit(DEFAULT_LIMIT), MAX_LIMIT);
        List<LogRecord> records = logSearchClient.search(request, limit);
        long hitCount = logSearchClient.count(request);
        List<LogQueryResult.RecentLogError> recentErrors = records.stream()
                .map(this::toRecentError)
                .toList();
        return new LogQueryResult(
                summary(request, hitCount, recentErrors.size()),
                hitCount,
                recentErrors,
                new LogQueryResult.TimeRangeResult(request.timeRange().from(), request.timeRange().to()));
    }

    private void validateTimeRange(LogQueryRequest request) {
        if (!request.timeRange().from().isBefore(request.timeRange().to())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "log query timeRange.from must be before timeRange.to");
        }
        Duration duration = Duration.between(request.timeRange().from(), request.timeRange().to());
        if (duration.compareTo(MAX_TIME_RANGE) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "log query time range cannot exceed 24 hours");
        }
    }

    private LogQueryResult.RecentLogError toRecentError(LogRecord record) {
        return new LogQueryResult.RecentLogError(
                record.timestamp(),
                record.serviceName(),
                record.level(),
                logMaskingService.mask(record.message()),
                record.traceId());
    }

    private String summary(LogQueryRequest request, long hitCount, int returnedCount) {
        if (hitCount == 0) {
            return "No logs matched serviceName=%s and keyword=%s in the requested time range."
                    .formatted(request.normalizedServiceName(), request.normalizedKeyword());
        }
        return "Found %d log entries for serviceName=%s and keyword=%s; returning %d recent snippets."
                .formatted(hitCount, request.normalizedServiceName(), request.normalizedKeyword(), returnedCount);
    }
}
