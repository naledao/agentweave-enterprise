package com.agentweave.observability.api;

import com.agentweave.observability.application.ObservabilitySummaryService;
import com.agentweave.observability.dto.ObservabilitySummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observability")
public class ObservabilitySummaryController {

    private final ObservabilitySummaryService observabilitySummaryService;

    public ObservabilitySummaryController(ObservabilitySummaryService observabilitySummaryService) {
        this.observabilitySummaryService = observabilitySummaryService;
    }

    @GetMapping("/summary")
    public ObservabilitySummaryResponse summary() {
        return observabilitySummaryService.summary();
    }
}
