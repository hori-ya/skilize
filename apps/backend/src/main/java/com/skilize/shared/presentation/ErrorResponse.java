package com.skilize.shared.presentation;

/**
 * 汎用エラーレスポンス。{ "code": "ERROR_CODE", "message": "エラーメッセージ" } の形式で返す。
 * code はフロントエンドでのエラー種別判定（例: "NOT_FOUND", "FORBIDDEN"）に使用する。
 */
public record ErrorResponse(String code, String message) {}
