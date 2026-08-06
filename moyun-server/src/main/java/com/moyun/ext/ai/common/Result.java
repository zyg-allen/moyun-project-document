package com.moyun.ext.ai.common;

/**
 * Result类 - ApiResponse的别名
 * 
 * <p>为了代码可读性,提供Result作为ApiResponse的简化别名</p>
 *
 * @param <T> 响应数据的类型
 * @author laomao
 */
public class Result<T> extends ApiResponse<T> {

    public Result(Boolean success, String message, T data, String errorCode) {
        super(success, message, data, errorCode);
    }

    /**
     * 创建成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(true, "操作成功", null, null);
    }

    /**
     * 创建成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data, null);
    }

    /**
     * 创建成功响应（带自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data, null);
    }

    /**
     * 创建失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null, null);
    }

    /**
     * 创建失败响应（带数据）
     */
    public static <T> Result<T> error(String message, T data) {
        return new Result<>(false, message, data, null);
    }
    
    /**
     * 创建成功响应（只有消息）
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(true, message, null, null);
    }
}
