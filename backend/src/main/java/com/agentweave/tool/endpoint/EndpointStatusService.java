package com.agentweave.tool.endpoint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndpointStatusService {

    private final EndpointRegistry endpointRegistry;
    private final EndpointStatusClient endpointStatusClient;

    public EndpointStatusService(
            EndpointRegistry endpointRegistry,
            EndpointStatusClient endpointStatusClient) {
        this.endpointRegistry = endpointRegistry;
        this.endpointStatusClient = endpointStatusClient;
    }

    @Transactional(readOnly = true)
    public EndpointStatusResult query(EndpointStatusRequest request) {
        EndpointRegistration registration = endpointRegistry.requireRegistered(request.normalizedEndpoint());
        return endpointStatusClient.query(registration);
    }
}
