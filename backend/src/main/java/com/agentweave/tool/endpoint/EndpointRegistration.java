package com.agentweave.tool.endpoint;

import java.util.Set;

public record EndpointRegistration(
        String endpoint,
        Set<String> aliases) {
}
