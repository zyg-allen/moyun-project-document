package com.moyun.common.exception.business.user;

import com.moyun.common.exception.system.BaseException;

public class UserException extends BaseException
{
    private static final long serialVersionUID = 1L;

    public UserException(String code, Object[] args)
    {
        super("user", code, args, null);
    }
}
