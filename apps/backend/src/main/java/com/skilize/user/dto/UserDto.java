package com.skilize.user.dto;

import com.skilize.user.domain.User;

import java.util.Map;

public record UserDto(int id, String userId, String name, String email, String role,
                      Integer tlUserId, String tlName, boolean isInitialPassword,
                      boolean isActive, String createdAt) {

    public static UserDto from(User u, Map<Integer, String> nameById) {
        return new UserDto(
                u.getId(), u.getUserId(), u.getName(), u.getEmail(),
                u.getRole().name(),
                u.getTlUserId(),
                u.getTlUserId() != null ? nameById.get(u.getTlUserId()) : null,
                u.isInitialPassword(), u.isActive(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }
}
