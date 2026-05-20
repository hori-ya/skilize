package com.skilize.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String userId,
        @NotBlank String name,
        String email,
        @NotBlank String role,
        Integer tlUserId
) {}
