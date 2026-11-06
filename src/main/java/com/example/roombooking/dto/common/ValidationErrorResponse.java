package com.example.roombooking.dto.common;

import java.time.LocalDateTime;
import java.util.List;

public record ValidationErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> errors
) {
    public record FieldErrorItem(String field, String message) {}
}