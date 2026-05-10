package com.skilize.auth.dto;

public record LoginResponse(
        String token,
        UserInfo user
) {
    public record UserInfo(
            Integer id,
            String name,
            String role,
            boolean isInitialPassword,
            TlUserInfo tlUser
    ) {}

    public record TlUserInfo(Integer id, String name) {}
}
