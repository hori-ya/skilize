package com.skilize.shared.domain.exception;

import java.util.List;

/**
 * 目標設定の件数バリデーション違反例外。
 * ITスキル/資格 ≥1 件・AD ≥2 件の条件を満たさずに目標完了操作を行った場合にスローされ、
 * GlobalExceptionHandler が 422 Unprocessable Entity として返す。
 */
public class GoalIncompleteException extends RuntimeException {

    private final List<GoalValidationError> errors;

    public GoalIncompleteException(List<GoalValidationError> errors) {
        super("目標設定の件数が不足しています");
        this.errors = errors;
    }

    public List<GoalValidationError> getErrors() {
        return errors;
    }

    public record GoalValidationError(String field, String message) {}
}
