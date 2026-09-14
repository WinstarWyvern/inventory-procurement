package com.procurement.api.common;

/**
 * Wraps every error body in the same shape:
 * {@code { "error": { "code": "...", "message": "...", "details": ... } } }
 */
public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, Object details) {
        public ErrorBody(String code, String message) {
            this(code, message, null);
        }
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public static ErrorResponse of(String code, String message, Object details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }
}
