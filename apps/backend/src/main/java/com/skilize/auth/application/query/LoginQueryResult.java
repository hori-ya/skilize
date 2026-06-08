/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ログイン成功時のクエリ結果オブジェクト。
 * JWT トークンとフロントエンドが必要とするユーザー情報をまとめて返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.auth.application.query;

/**
 * ログイン成功時のクエリ結果。JWT トークンとユーザー情報をまとめて返す。
 * フロントエンドは token を localStorage に保存し、以降のリクエストで Bearer 認証に使用する。
 *
 * @param token 発行した JWT アクセストークン
 * @param user  ログインユーザー情報（ロール・初回PW フラグ・TL情報を含む）
 */
public record LoginQueryResult(
        String token,
        UserInfo user
) {
    /**
     * ログイン後にフロントエンドが保持するユーザー情報。
     *
     * @param id                ユーザーの内部 PK
     * @param name              氏名
     * @param role              ロール（GENERAL / TL / ADMIN）
     * @param isInitialPassword 初回パスワードフラグ（true なら change-password 以外へのアクセスを 403 で拒否）
     * @param tlUser            上長（TL）情報。GENERAL ロールの場合のみ設定。TL/ADMIN は null
     */
    public record UserInfo(
            Integer id,
            String name,
            String role,
            boolean isInitialPassword,
            TlUserInfo tlUser
    ) {}

    /**
     * 上長（TL）の最小情報。チームリーダー名の表示用途のみに使う軽量なネスト型。
     *
     * @param id   上長の内部 PK
     * @param name 上長の氏名
     */
    public record TlUserInfo(Integer id, String name) {}
}
