package com.moyun.agent.exception;

/**
 * 业务异常类
 *
 * <p>用于封装业务逻辑中的异常情况，支持：
 * <ul>
 *   <li>错误码 + 错误消息</li>
 *   <li>支持ErrorCode枚举</li>
 *   <li>支持自定义消息覆盖</li>
 *   <li>支持异常链</li>
 * </ul>
 * </p>
 *
 * <p>使用示例：
 * <pre>
 * // 使用ErrorCode枚举
 * throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
 *
 * // 使用ErrorCode + 自定义消息
 * throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED, "PDF文件损坏");
 *
 * // 带原始异常
 * throw new BusinessException(ErrorCode.VECTOR_STORE_FAILED, e);
 * </pre>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String msg;

    /**
     * 使用ErrorCode构造
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }

    /**
     * 使用ErrorCode + 自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.msg = message;
    }

    /**
     * 使用ErrorCode + 原始异常构造
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }

    /**
     * 使用ErrorCode + 自定义消息 + 原始异常构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.msg = message;
    }

    /**
     * 使用错误码和消息直接构造
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误消息
     *
     * @return 错误消息
     */
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "BusinessException{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
