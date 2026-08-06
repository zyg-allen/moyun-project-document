package com.moyun.ext.ai.exception;

/**
 * 图片过滤异常
 * 
 * <p>在图片过滤过程中发生的异常</p>
 * 
 * @author laomao
 */
public class ImageFilterException extends RuntimeException {

    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public ImageFilterException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ImageFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * 
     * @param cause 原因异常
     */
    public ImageFilterException(Throwable cause) {
        super(cause);
    }
}
