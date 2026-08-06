package com.moyun.ext.ai.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应封装类
 *
 * <p>用于替代Map<String, Object>的响应格式，提供类型安全和更好的代码可读性</p>
 *
 * @param <T> 响应数据的类型
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 请求是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 错误代码（可选）
     */
    private String errorCode;

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "操作成功", null, null);
    }

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data, null);
    }

    /**
     * 创建成功响应（带自定义消息）
     *
     * @param message 成功消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param message 成功消息
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * 创建失败响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    /**
     * 创建失败响应（带数据）
     *
     * @param message 错误消息
     * @param data 错误详情数据
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, null);
    }

    /**
     * 创建失败响应（带错误代码）
     *
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> errorWithCode(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }

    /**
     * 创建失败响应（带数据和错误代码）
     *
     * @param message 错误消息
     * @param data 错误详情数据
     * @param errorCode 错误代码
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> error(String message, T data, String errorCode) {
        return new ApiResponse<>(false, message, data, errorCode);
    }
}
