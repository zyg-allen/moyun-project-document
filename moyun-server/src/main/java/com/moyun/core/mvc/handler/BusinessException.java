package com.moyun.core.mvc.handler;

import lombok.Getter;

/**
 * <p>
 * 自定义业务异常类
 * </p>
 *
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
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造函数
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数（默认错误码000001）
     *
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
     * <p><b>修复说明</b>：原实现返回 {@code String.format("[%s] %s", errorCode, errorMessage)}，
     * 但 {@code errorCode}/{@code errorMessage} 这两个字段**从未在任何构造函数中赋值**（构造函数写的是
     * {@code code}/{@code message}）⇒ 任何 {@code BusinessException} 的 {@code getMessage()} 恒为
     * {@code "[null] null"}，交给全局异常处理器后前端只能拿到无意义字符串。
     * 现改为返回构造函数传入的真实提示，仅在缺失时回退父类。</p>
     *
     * @return 完整错误信息字符串
     */
    @Override
    public String getMessage() {
        if (this.message != null && !this.message.isBlank()) {
            return this.message;
        }
        return super.getMessage();
    }
}
