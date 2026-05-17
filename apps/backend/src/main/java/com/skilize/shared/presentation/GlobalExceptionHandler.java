package com.skilize.shared.presentation;

import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
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
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e) {
        // AuthException の code が "FORBIDDEN" のみ 403 を返す。それ以外（AUTH_FAILED 等）は 401
        HttpStatus status = "FORBIDDEN".equals(e.getCode()) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
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
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(code, message));
    }

    @ExceptionHandler(GoalIncompleteException.class)
    public ResponseEntity<GoalIncompleteResponse> handleGoalIncomplete(GoalIncompleteException e) {
        // 422 Unprocessable Entity: 目標件数不足を専用コードで返す
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new GoalIncompleteResponse("GOAL_INCOMPLETE", e.getMessage(), e.getErrors()));
    }

    public record GoalIncompleteResponse(String code, String message,
                                          List<GoalIncompleteException.GoalValidationError> errors) {}
}
