package com.skilize.auth.dto;

public record MeResponse(
        Integer id,
        String userId,
        String name,
        String email,
        String role,
        boolean isInitialPassword,
        LoginResponse.TlUserInfo tlUser,
        boolean isActive
) {}
