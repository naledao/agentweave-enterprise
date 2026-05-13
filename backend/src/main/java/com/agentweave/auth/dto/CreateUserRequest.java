package com.agentweave.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(min = 8, max = 200) String password,
        @Email @Size(max = 255) String email,
        @NotEmpty Set<UUID> roleIds) {
}
