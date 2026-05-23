package com.moyun.common.utils;

import com.moyun.common.utils.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 日志工具类
 *
 * @author ruoyi
 */
public class LogUtils {

    public static String getBlock(Object msg)
    {
        if (msg == null)
        {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }

    /**
     * 获取Logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 获取Logger
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * 获取当前请求的IP地址
     */
    public static String getIp() {
        return IpUtils.getIpAddr();
    }

    /**
     * 获取当前请求的URL
     */
    public static String getUrl() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURL().toString() : "";
    }

    /**
     * 获取当前请求的方法
     */
    public static String getMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : "";
    }

    /**
     * 获取当前请求的参数
     */
    public static String getParams() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        return request.getQueryString() != null ? request.getQueryString() : "";
    }

    /**
     * 获取当前请求
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 记录debug日志
     */
    public static void debug(Logger logger, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    /**
     * 记录debug日志
     */
    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, args);
        }
    }

    /**
     * 记录info日志
     */
    public static void info(Logger logger, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    /**
     * 记录info日志
     */
    public static void info(Logger logger, String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(format, args);
        }
    }

    /**
     * 记录warn日志
     */
    public static void warn(Logger logger, String message) {
        if (logger.isWarnEnabled()) {
            logger.warn(message);
        }
    }

    /**
     * 记录warn日志
     */
    public static void warn(Logger logger, String format, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(format, args);
        }
    }

    /**
     * 记录warn日志
     */
    public static void warn(Logger logger, String message, Throwable e) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, e);
        }
    }

    /**
     * 记录error日志
     */
    public static void error(Logger logger, String message) {
        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
    }

    /**
     * 记录error日志
     */
    public static void error(Logger logger, String format, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(format, args);
        }
    }

    /**
     * 记录error日志
     */
    public static void error(Logger logger, String message, Throwable e) {
        if (logger.isErrorEnabled()) {
            logger.error(message, e);
        }
    }
}