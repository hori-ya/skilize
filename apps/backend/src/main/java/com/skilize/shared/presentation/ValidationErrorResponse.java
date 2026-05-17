package com.skilize.shared.presentation;

import java.util.List;

/**
 * バリデーションエラーレスポンス。@Valid 失敗時に GlobalExceptionHandler が返す。
 * { "code": "VALIDATION_ERROR", "message": "...", "errors": [{ "field": "...", "message": "..." }] } の形式。
 * フロントエンドでフィールドごとのエラーメッセージ表示に使用する。
 */
public record ValidationErrorResponse(String code, String message, List<FieldError> errors) {
    /** フィールド名と対応するエラーメッセージのペア。 */
    public record FieldError(String field, String message) {}
}
