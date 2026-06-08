/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 認証系リクエスト DTO を Application 層のコマンドオブジェクトに変換するマッパークラス。
 * presentation 層から application 層への依存を防ぐ変換処理を担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.auth.application.mapper;

import com.skilize.auth.application.command.ChangePasswordCommand;
import com.skilize.auth.application.command.LoginCommand;
import com.skilize.auth.presentation.request.ChangePasswordRequest;
import com.skilize.auth.presentation.request.LoginRequest;
import org.springframework.stereotype.Component;

/**
 * 認証系リクエストを Application 層コマンドに変換するマッパー。
 * Presentation 層の Request クラスを Service 層に渡さないようにするための変換クラス。
 */
@Component
public class AuthApplicationMapper {

    /**
     * ログインリクエストをログインコマンドに変換する。
     * @param request POST /api/auth/login のリクエストボディ
     * @return AuthService.login() に渡すコマンドオブジェクト
     */
    public LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.userId(), request.password());
    }

    /**
     * パスワード変更リクエストをパスワード変更コマンドに変換する。
     * @param request POST /api/auth/change-password のリクエストボディ
     * @return AuthService.changePassword() に渡すコマンドオブジェクト
     */
    public ChangePasswordCommand toCommand(ChangePasswordRequest request) {
        return new ChangePasswordCommand(request.currentPassword(), request.newPassword());
    }
}
