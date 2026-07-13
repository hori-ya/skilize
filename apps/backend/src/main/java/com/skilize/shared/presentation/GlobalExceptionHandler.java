/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * アプリケーション全体の例外を統一フォーマットの HTTP レスポンスに変換するハンドラー。
 * 認証エラー・バリデーションエラー・HTTP ステータスエラー・予期しない例外を一元的に処理する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.presentation;

import com.skilize.master.infrastructure.excel.ExcelFormatException;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * アプリケーション全体の例外を統一フォーマットの HTTP レスポンスに変換するハンドラー。
 * エラーレスポンス形式: { "code": "ERROR_CODE", "message": "..." }
 * バリデーションエラー: { "code": "VALIDATION_ERROR", "message": "...", "errors": [...] }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 認証・認可例外を処理する。
     * FORBIDDEN / ACCOUNT_DISABLED は 403、その他（AUTH_FAILED 等）は 401 を返す。
     * @param e 認証・認可エラー例外
     * @return エラーコードを含む HTTP レスポンス
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e) {
        // FORBIDDEN / ACCOUNT_DISABLED は 403、それ以外（AUTH_FAILED 等）は 401
        boolean isForbidden = "FORBIDDEN".equals(e.getCode()) || "ACCOUNT_DISABLED".equals(e.getCode());
        HttpStatus status;
        if (isForbidden) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.UNAUTHORIZED;
        }
        log.warn("Auth error: code={}", e.getCode());
        return ResponseEntity.status(status).body(new ErrorResponse(e.getCode(), ""));
    }

    /**
     * @Valid によるバリデーション失敗を処理する。
     * フィールドごとのエラーコードをリストにまとめて 400 Bad Request で返す。
     * @param e バリデーション失敗例外
     * @return フィールドエラー詳細を含む HTTP レスポンス
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorResponse.FieldError> errors = new ArrayList<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            // コード配列は "Size.request.field", "Size.field", "Size.java.lang.String", "Size" の順に並ぶ
            // 最後の要素（アノテーション名）をエラーコードとして使用する
            String[] codes = fe.getCodes();
            String code = "VALIDATION_FAILED";
            if (codes != null && codes.length > 0) {
                code = codes[codes.length - 1];
            }
            errors.add(new ValidationErrorResponse.FieldError(fe.getField(), code));
        }
        log.warn("Validation error: {} field(s) failed", errors.size());
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "", errors));
    }

    /**
     * ResponseStatusException（サービス層が throw する HTTP ステータスエラー）を処理する。
     * detail フィールドのエラーコード文字列をそのままレスポンスに使用する。
     * @param e HTTP ステータス例外
     * @return エラーコードと対応するステータスの HTTP レスポンス
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        // サービス側がエラーコード文字列を detail に設定して throw するため、それをそのまま使用する
        String detail = e.getBody().getDetail();
        String code;
        if (detail != null && !detail.isBlank()) {
            code = detail;
        } else {
            switch (e.getStatusCode().value()) {
                case 404:
                    code = "NOT_FOUND";
                    break;
                case 403:
                    code = "FORBIDDEN";
                    break;
                case 409:
                    code = "CONFLICT";
                    break;
                default:
                    code = "ERROR";
                    break;
            }
        }
        log.warn("HTTP error: status={} code={}", e.getStatusCode().value(), code);
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(code, ""));
    }

    /**
     * Excel ファイルフォーマットエラーを処理する。
     * マスターデータインポート時のフォーマット違反を 400 Bad Request で返す。
     * @param e Excel フォーマット例外
     * @return エラーコードを含む 400 HTTP レスポンス
     */
    @ExceptionHandler(ExcelFormatException.class)
    public ResponseEntity<ErrorResponse> handleExcelFormat(ExcelFormatException e) {
        log.warn("Excel format error: code={}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage(), ""));
    }

    /**
     * 目標設定未完了例外を処理する。
     * 件数条件を満たさずに目標完了操作を行った場合に 422 Unprocessable Entity を返す。
     * @param e 目標未完了例外
     * @return バリデーション違反詳細を含む 422 HTTP レスポンス
     */
    @ExceptionHandler(GoalIncompleteException.class)
    public ResponseEntity<GoalIncompleteResponse> handleGoalIncomplete(GoalIncompleteException e) {
        log.warn("Goal incomplete");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new GoalIncompleteResponse("GOAL_INCOMPLETE", "", e.getErrors()));
    }

    /**
     * 上記以外の予期しない例外を処理する。
     * スタックトレースをログに出力し、500 Internal Server Error を返す。
     * @param e 予期しない例外
     * @return INTERNAL_ERROR コードを含む 500 HTTP レスポンス
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", ""));
    }

    public record GoalIncompleteResponse(String code, String message,
                                          List<GoalIncompleteException.GoalValidationError> errors) {}
}
