package com.moyun.util.string;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

    public static String getExceptionMessage(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.getBuffer().toString();
    }

    public static Throwable getRootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static String getRootCauseMessage(Throwable e) {
        return getRootCause(e).getMessage();
    }
}
