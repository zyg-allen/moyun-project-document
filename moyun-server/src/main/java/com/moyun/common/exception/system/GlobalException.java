package com.moyun.common.exception.system;

public class GlobalException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String message;

    private String detailMessage;

    private Integer code;

    public GlobalException() {
    }

    public GlobalException(String message) {
        this.message = message;
    }

    public GlobalException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public GlobalException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public GlobalException setMessage(String message) {
        this.message = message;
        return this;
    }

    public Integer getCode() {
        return code;
    }

    public GlobalException setCode(Integer code) {
        this.code = code;
        return this;
    }
}
