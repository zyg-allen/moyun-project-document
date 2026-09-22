package com.moyun.common.exception.system.file;

public class FileUploadException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final Throwable cause;

    public FileUploadException()
    {
        this(null, null);
    }

    public FileUploadException(final String msg)
    {
        this(msg, null);
    }

    public FileUploadException(String msg, Throwable cause)
    {
        // cause 交给 Throwable 基类管理，标准 printStackTrace/日志会自带 "Caused by" 链，
        // 无需再手写 printStackTrace 重定向（原重写直接打印到 stdout/stderr，绕过了日志框架）
        super(msg, cause);
        this.cause = cause;
    }

    @Override
    public Throwable getCause()
    {
        return cause;
    }
}
