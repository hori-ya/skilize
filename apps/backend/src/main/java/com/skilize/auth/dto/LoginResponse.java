package com.skilize.auth.dto;

/**
 * ログイン成功レスポンス。token は localStorage に保存し、以降のリクエストで Bearer トークンとして使用する。
 * UserInfo はログイン後の画面表示・ロール制御に必要な情報のみを含む（passwordHash 等は除外）。
 */
public record LoginResponse(
        String token,
        UserInfo user
) {
    /** ログイン後に使用するユーザー情報（ロール・初回PW フラグ・TL情報を含む）。 */
    public record UserInfo(
            Integer id,
            String name,
            String role,
            boolean isInitialPassword,
            TlUserInfo tlUser
    ) {}

    /** 上長（TL）の ID と氏名のみを返す軽量なネスト DTO。 */
    public record TlUserInfo(Integer id, String name) {}
}
