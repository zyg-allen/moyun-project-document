package com.moyun.common.exception.system.file;

import com.moyun.common.exception.system.BaseException;

public class FileException extends BaseException
{
    private static final long serialVersionUID = 1L;

    public FileException(String code, Object[] args)
    {
        super("file", code, args, null);
    }

}
