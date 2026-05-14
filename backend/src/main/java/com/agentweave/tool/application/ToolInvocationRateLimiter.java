package com.agentweave.tool.application;

import com.agentweave.shared.exception.TooManyRequestsException;
import com.agentweave.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ToolInvocationRateLimiter {

    private final ToolSecurityProperties properties;
    private final Clock clock;
    private final Map<String, ConcurrentLinkedDeque<Instant>> invocationWindows = new ConcurrentHashMap<>();

    @Autowired
    public ToolInvocationRateLimiter(ToolSecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    ToolInvocationRateLimiter(ToolSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void requireAllowed(CurrentUser user, String permissionCode) {
        Instant now = clock.instant();
        Instant windowStart = now.minusSeconds(60);
        String key = user.id() + ":" + permissionCode;
        ConcurrentLinkedDeque<Instant> invocations =
                invocationWindows.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (invocations) {
            evictExpired(invocations, windowStart);
            if (invocations.size() >= properties.maxInvocationsPerMinute()) {
                throw new TooManyRequestsException("tool invocation rate limit exceeded");
            }
            invocations.addLast(now);
        }
    }

    private void evictExpired(ConcurrentLinkedDeque<Instant> invocations, Instant windowStart) {
        Iterator<Instant> iterator = invocations.iterator();
        while (iterator.hasNext()) {
            Instant invocation = iterator.next();
            if (invocation.isAfter(windowStart)) {
                break;
            }
            iterator.remove();
        }
    }
}
