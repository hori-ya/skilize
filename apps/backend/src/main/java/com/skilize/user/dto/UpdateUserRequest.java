package com.skilize.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        @NotBlank String name,
        String email,
        @NotBlank @Pattern(regexp = "GENERAL|TL|ADMIN") String role,
        Integer tlUserId,
        Boolean active
) {}
