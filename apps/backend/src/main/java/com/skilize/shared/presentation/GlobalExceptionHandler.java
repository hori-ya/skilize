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
        // AuthException の code が "FORBIDDEN" のみ 403 を返す。それ以外（AUTH_FAILED 等）は 401
        HttpStatus status = "FORBIDDEN".equals(e.getCode()) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        log.warn("Auth error: code={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(status).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Validation error: {} field(s) failed", errors.size());
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "入力値に誤りがあります", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        String code = switch (e.getStatusCode().value()) {
            case 404 -> "NOT_FOUND";
            case 403 -> "FORBIDDEN";
            case 409 -> "CONFLICT";
            default -> "ERROR";
        };
        String detail = e.getBody().getDetail();
        String message = detail != null ? detail : e.getMessage();
        log.warn("HTTP error: status={} code={} message={}", e.getStatusCode().value(), code, message);
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(code, message));
    }

    @ExceptionHandler(ExcelFormatException.class)
    public ResponseEntity<ErrorResponse> handleExcelFormat(ExcelFormatException e) {
        log.warn("Excel format error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("EXCEL_FORMAT_ERROR", e.getMessage()));
    }

    @ExceptionHandler(GoalIncompleteException.class)
    public ResponseEntity<GoalIncompleteResponse> handleGoalIncomplete(GoalIncompleteException e) {
        log.warn("Goal incomplete: {}", e.getMessage());
        // 422 Unprocessable Entity: 目標件数不足を専用コードで返す
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new GoalIncompleteResponse("GOAL_INCOMPLETE", e.getMessage(), e.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "予期しないエラーが発生しました"));
    }

    public record GoalIncompleteResponse(String code, String message,
                                          List<GoalIncompleteException.GoalValidationError> errors) {}
}
