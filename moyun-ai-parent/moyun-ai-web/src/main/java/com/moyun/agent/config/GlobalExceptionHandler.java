package com.moyun.agent.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一处理Sa-Token认证异常，返回JSON格式响应</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理未登录异常
     *
     * <p>根据不同的未登录类型返回对应的错误信息，HTTP状态码为401</p>
     *
     * @param e 未登录异常
     * @return 错误响应
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLoginException(NotLoginException e) {
        String message;
        // 根据异常类型返回不同的提示信息
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = "未提供Token";
                break;
            case NotLoginException.INVALID_TOKEN:
                message = "Token无效";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "Token已过期";
                break;
            case NotLoginException.BE_REPLACED:
                message = "账号已在其他地方登录";
                break;
            case NotLoginException.KICK_OUT:
                message = "账号已被踢下线";
                break;
            default:
                message = "未登录";
        }
        log.warn("认证失败: {}", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    /**
     * 处理无权限异常
     *
     * <p>当用户没有访问某个接口的权限时触发，HTTP状态码为403</p>
     *
     * @param e 无权限异常
     * @return 错误响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限不足: {}", e.getPermission());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("没有权限访问"));
    }

    /**
     * 处理无角色异常
     *
     * <p>当用户没有所需角色时触发，HTTP状态码为403</p>
     *
     * @param e 无角色异常
     * @return 错误响应
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotRoleException(NotRoleException e) {
        log.warn("角色不足: {}", e.getRole());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("没有权限访问"));
    }
    
    /**
     * 处理参数校验异常（@Valid注解校验失败）
     * 
     * <p>当请求体参数校验失败时触发，返回具体的校验错误信息</p>
     * 
     * @param e 方法参数校验异常
     * @return 错误响应，包含所有校验失败的字段和错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        log.warn("参数校验失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("参数校验失败: " + errors));
    }
    
    /**
     * 处理约束违反异常（@Validated注解校验失败）
     * 
     * @param e 约束违反异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("约束校验失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("参数校验失败: " + errors));
    }
    
    /**
     * 处理绑定异常（表单数据绑定失败）
     * 
     * @param e 绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String errors = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        log.warn("数据绑定失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("参数错误: " + errors));
    }
    
    /**
     * 处理非法参数异常
     * 
     * @param e 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }
    
    /**
     * 处理业务异常
     * 
     * <p>处理业务逻辑中主动抛出的BusinessException，返回对应的错误码和消息</p>
     * 
     * @param e 业务异常
     * @return 错误响应，包含业务错误码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMsg());
        // 根据错误码决定HTTP状态码
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getCode() >= 60001 && e.getCode() <= 60005) {
            // 认证相关错误使用401/403
            status = e.getCode() == 60005 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        }
        return ResponseEntity.status(status)
                .body(ApiResponse.errorWithCode(e.getMsg(), String.valueOf(e.getCode())));
    }
    
    /**
     * 处理运行时异常
     * 
     * <p>捕获业务逻辑抛出的RuntimeException，返回500错误</p>
     * 
     * @param e 运行时异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("操作失败: " + e.getMessage()));
    }
    
    /**
     * 处理所有未捕获的异常
     * 
     * <p>作为兜底异常处理，避免异常信息直接暴露给前端</p>
     * 
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("❌ 系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统错误，请稍后重试"));
    }
}
