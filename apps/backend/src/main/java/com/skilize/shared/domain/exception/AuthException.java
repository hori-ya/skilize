/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 認証・認可エラー用の汎用例外クラス。
 * エラーコードによって GlobalExceptionHandler が 401 または 403 を返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.domain.exception;

import lombok.Getter;

/**
 * 認証・認可エラー用の汎用例外。
 * code="FORBIDDEN" の場合 GlobalExceptionHandler が 403 を返し、それ以外は 401 を返す。
 */
@Getter
public class AuthException extends RuntimeException {

    private final String code;

    /**
     * 認証・認可例外を生成する。
     * @param code    エラーコード（例: "AUTH_FAILED", "FORBIDDEN", "ACCOUNT_DISABLED"）
     * @param message エラーメッセージ（外部には公開しないため空文字を渡すことが多い）
     */
    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }
}
