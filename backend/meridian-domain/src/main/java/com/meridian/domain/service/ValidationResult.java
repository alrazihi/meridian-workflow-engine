package com.meridian.domain.service;

public record ValidationResult(boolean isValid, String errorMessage) {
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
