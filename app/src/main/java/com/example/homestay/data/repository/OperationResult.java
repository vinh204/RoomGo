package com.example.homestay.data.repository;

public final class OperationResult<T> {
    private final T value;
    private final Exception error;

    private OperationResult(T value, Exception error) {
        this.value = value;
        this.error = error;
    }

    public static <T> OperationResult<T> success(T value) {
        return new OperationResult<>(value, null);
    }

    public static <T> OperationResult<T> failure(Exception error) {
        return new OperationResult<>(null, error);
    }

    public boolean isSuccess() { return error == null; }
    public T getValue() { return value; }
    public Exception getError() { return error; }
}
