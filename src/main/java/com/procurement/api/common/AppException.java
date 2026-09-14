package com.procurement.api.common;

import org.springframework.http.HttpStatus;

/**
 * A structured application error that carries an HTTP status and a stable,
 * machine-readable code, so API consumers can branch on {@code error.code}
 * instead of parsing the human-readable message.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Object details;

    public AppException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public AppException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }

    // ---- Common factory helpers, mirroring the API's error contract ----

    public static AppException unauthorized(String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static AppException invalidCredentials() {
        return new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Username or password is incorrect");
    }

    public static AppException forbidden(String message) {
        return new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static AppException notFound(String resource) {
        return new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " not found");
    }

    public static AppException conflict(String code, String message) {
        return new AppException(HttpStatus.CONFLICT, code, message);
    }

    public static AppException badRequest(String code, String message) {
        return new AppException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static AppException badRequest(String code, String message, Object details) {
        return new AppException(HttpStatus.BAD_REQUEST, code, message, details);
    }
}
