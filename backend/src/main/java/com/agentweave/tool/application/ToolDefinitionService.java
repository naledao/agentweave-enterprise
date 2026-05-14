package com.agentweave.tool.application;

import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.dto.ToolDefinitionResponse;
import com.agentweave.tool.repository.ToolDefinitionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolDefinitionService {

    private final ToolDefinitionRepository toolDefinitionRepository;
    private final CurrentUserService currentUserService;

    public ToolDefinitionService(
            ToolDefinitionRepository toolDefinitionRepository,
            CurrentUserService currentUserService) {
        this.toolDefinitionRepository = toolDefinitionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ToolDefinitionResponse> listTools() {
        CurrentUser user = currentUserService.requireCurrentUser();
        return toolDefinitionRepository.findAllByOrderByCodeAsc().stream()
                .map(definition -> ToolDefinitionResponse.from(definition, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireEnabledToolForPermission(String permissionCode) {
        ToolDefinitionEntity definition = toolDefinitionRepository.findByPermissionCode(permissionCode)
                .orElseThrow(() -> new ResourceNotFoundException("tool definition not found"));
        if (!definition.isEnabled()) {
            throw new AccessDeniedBusinessException("tool is disabled");
        }
    }
}
