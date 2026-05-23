package com.moyun.common.enums;

import java.util.function.Function;

/**
 * 脱敏类型
 *
 * @author ruoyi
 */
public enum DesensitizedType {
    /**
     * 姓名，第2位开始脱敏
     */
    NAME(s -> s.replaceAll("(?<=.{1}).", "*")),

    /**
     * 身份证，中间10位脱敏
     */
    ID_CARD(s -> s.replaceAll("(?<=.{3}).(?=.{4})", "*")),

    /**
     * 手机号，中间4位脱敏
     */
    PHONE(s -> s.replaceAll("(?<=.{3}).(?=.{4})", "*")),

    /**
     * 电子邮箱，@前保留前3位，后脱敏
     */
    EMAIL(s -> s.replaceAll("(?<=.{3}).(?=@)", "*")),

    /**
     * 银行卡号，保留后4位
     */
    BANK_CARD(s -> s.replaceAll("(?<=.{4}).", "*")),

    /**
     * 地址，保留前6位
     */
    ADDRESS(s -> s.replaceAll("(?<=.{6}).", "*")),

    /**
     * 密码，全部脱敏
     */
    PASSWORD(s -> s.replaceAll(".", "*"));

    private final Function<String, String> desensitizer;

    DesensitizedType(Function<String, String> desensitizer) {
        this.desensitizer = desensitizer;
    }

    public Function<String, String> desensitizer() {
        return desensitizer;
    }
}