package com.skilize.auth.dto;

/**
 * GET /api/auth/me のレスポンス。JWT から復元したユーザー情報をフロントエンドに返す。
 * アプリ起動時のセッション復元・ロール確認・初回PW 判定に使用する。
 */
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
