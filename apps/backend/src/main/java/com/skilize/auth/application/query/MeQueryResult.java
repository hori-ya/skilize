/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * GET /api/auth/me のクエリ結果オブジェクト。
 * JWT から復元した認証済みユーザーの詳細情報をフロントエンドに返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.auth.application.query;

/**
 * GET /api/auth/me のクエリ結果。JWT から復元したユーザー情報をフロントエンドに返す。
 * アプリ起動時のセッション復元・ロール確認・初回PW 判定に使用する。
 *
 * @param id                ユーザーの内部 PK
 * @param userId            ログインID（例: "user01"）。内部 PK ではない
 * @param name              氏名
 * @param email             メールアドレス（未設定の場合は null）
 * @param role              ロール（GENERAL / TL / ADMIN）
 * @param isInitialPassword 初回パスワードフラグ（true なら change-password 以外は 403）
 * @param tlUser            上長（TL）情報（GENERAL の場合のみ設定。TL/ADMIN は null）
 * @param isActive          有効フラグ（false の場合はログイン不可）
 */
public record MeQueryResult(
        Integer id,
        String userId,
        String name,
        String email,
        String role,
        boolean isInitialPassword,
        LoginQueryResult.TlUserInfo tlUser,
        boolean isActive
) {}
