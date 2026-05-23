package com.moyun.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常工具类
 *
 * @author ruoyi
 */
public class ExceptionUtil {

    /**
     * 获取异常信息
     */
    public static String getExceptionMessage(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    /**
     * 获取异常堆栈信息
     */
    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.getBuffer().toString();
    }

    /**
     * 获取异常根因
     */
    public static Throwable getRootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 获取异常根因信息
     */
    public static String getRootCauseMessage(Throwable e) {
        return getRootCause(e).getMessage();
    }
}