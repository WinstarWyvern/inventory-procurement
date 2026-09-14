package com.procurement.api.common;

/** Wraps every successful response body as {@code { "data": ... } }. */
public record DataResponse<T>(T data) {
    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data);
    }
}
