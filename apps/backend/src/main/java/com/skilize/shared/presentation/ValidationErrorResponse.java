package com.skilize.shared.presentation;

import java.util.List;

public record ValidationErrorResponse(String code, String message, List<FieldError> errors) {
    public record FieldError(String field, String message) {}
}
