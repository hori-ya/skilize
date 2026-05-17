package com.skilize.shared.domain.exception;

import lombok.Getter;

/**
 * 認証・認可エラー用の汎用例外。
 * code="FORBIDDEN" の場合 GlobalExceptionHandler が 403 を返し、それ以外は 401 を返す。
 */
@Getter
public class AuthException extends RuntimeException {

    private final String code;

    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }
}
