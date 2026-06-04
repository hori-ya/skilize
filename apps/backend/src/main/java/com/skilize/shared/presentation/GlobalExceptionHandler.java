package com.skilize.shared.presentation;

import com.skilize.master.infrastructure.excel.ExcelFormatException;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * アプリケーション全体の例外を統一フォーマットの HTTP レスポンスに変換するハンドラー。
 * エラーレスポンス形式: { "code": "ERROR_CODE", "message": "..." }
 * バリデーションエラー: { "code": "VALIDATION_ERROR", "message": "...", "errors": [...] }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e) {
        // FORBIDDEN / ACCOUNT_DISABLED は 403、それ以外（AUTH_FAILED 等）は 401
        boolean isForbidden = "FORBIDDEN".equals(e.getCode()) || "ACCOUNT_DISABLED".equals(e.getCode());
        HttpStatus status = isForbidden ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        log.warn("Auth error: code={}", e.getCode());
        return ResponseEntity.status(status).body(new ErrorResponse(e.getCode(), ""));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    // コード配列は "Size.request.field", "Size.field", "Size.java.lang.String", "Size" の順に並ぶ
                    // 最後の要素（アノテーション名）をエラーコードとして使用する
                    String[] codes = fe.getCodes();
                    String code = (codes != null && codes.length > 0)
                            ? codes[codes.length - 1] : "VALIDATION_FAILED";
                    return new ValidationErrorResponse.FieldError(fe.getField(), code);
                })
                .toList();
        log.warn("Validation error: {} field(s) failed", errors.size());
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        // サービス側がエラーコード文字列を detail に設定して throw するため、それをそのまま使用する
        String detail = e.getBody().getDetail();
        String code = (detail != null && !detail.isBlank()) ? detail : switch (e.getStatusCode().value()) {
            case 404 -> "NOT_FOUND";
            case 403 -> "FORBIDDEN";
            case 409 -> "CONFLICT";
            default -> "ERROR";
        };
        log.warn("HTTP error: status={} code={}", e.getStatusCode().value(), code);
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(code, ""));
    }

    @ExceptionHandler(ExcelFormatException.class)
    public ResponseEntity<ErrorResponse> handleExcelFormat(ExcelFormatException e) {
        log.warn("Excel format error: code={}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage(), ""));
    }

    @ExceptionHandler(GoalIncompleteException.class)
    public ResponseEntity<GoalIncompleteResponse> handleGoalIncomplete(GoalIncompleteException e) {
        log.warn("Goal incomplete");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new GoalIncompleteResponse("GOAL_INCOMPLETE", "", e.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", ""));
    }

    public record GoalIncompleteResponse(String code, String message,
                                          List<GoalIncompleteException.GoalValidationError> errors) {}
}
