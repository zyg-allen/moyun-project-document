package com.moyun.common.exception.system;

public class NonCaptureException extends RuntimeException {
    public NonCaptureException(String message, Throwable cause) {
        super(message, cause);
    }
}
