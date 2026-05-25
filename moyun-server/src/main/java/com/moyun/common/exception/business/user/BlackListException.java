package com.moyun.common.exception.business.user;

public class BlackListException extends UserException {
    private static final long serialVersionUID = 1L;

    public BlackListException() {
        super("您已被列入黑名单，请联系管理员",null);
    }
}
