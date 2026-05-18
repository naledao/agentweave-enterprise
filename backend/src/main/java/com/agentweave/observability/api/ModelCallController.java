package com.agentweave.observability.api;

import com.agentweave.observability.application.ModelCallQueryService;
import com.agentweave.observability.dto.ModelCallListResponse;
import com.agentweave.observability.dto.ModelCallQueryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observability/model-calls")
public class ModelCallController {

    private final ModelCallQueryService modelCallQueryService;

    public ModelCallController(ModelCallQueryService modelCallQueryService) {
        this.modelCallQueryService = modelCallQueryService;
    }

    @GetMapping
    public ModelCallListResponse list(@Valid ModelCallQueryRequest request) {
        return modelCallQueryService.list(request);
    }
}
