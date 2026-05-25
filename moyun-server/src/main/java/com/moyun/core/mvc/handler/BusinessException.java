package com.moyun.core.mvc.handler;

import lombok.Getter;

/**
 * <p>
 *   自定义业务异常类
 * </p>
 * @author Lenovo
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误描述信息
     */
    private  String errorMessage;

    /**
     * 错误码
     */
    private  String errorCode;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数（默认错误码000001）
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = "000001";
        this.message = message;
    }

    /**
     * 获取完整的错误信息
     *
     * @return 完整错误信息字符串
     */
    @Override
    public String getMessage() {
        return String.format("[%s] %s", this.errorCode, this.errorMessage);
    }
}
