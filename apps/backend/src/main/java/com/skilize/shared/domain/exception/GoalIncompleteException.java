package com.skilize.shared.domain.exception;

import java.util.List;

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
