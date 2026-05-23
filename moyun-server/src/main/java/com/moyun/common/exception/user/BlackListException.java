package com.moyun.common.exception.user;

/**
 * 黑名单IP异常类
 *
 * @author ruoyi
 */
public class BlackListException extends com.moyun.common.exception.user.UserException {
    private static final long serialVersionUID = 1L;

    public BlackListException() {
        super("您已被列入黑名单，请联系管理员",null);
    }
}