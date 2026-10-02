package com.moyun.ledger.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 记账账户接口的请求体字段转换（包内共用）
 *
 * <p>为什么账户"修改"接口用 {@code Map<String, Object>} 接参而不是直接绑定实体：
 * 必须区分「**请求体里没这个字段**」与「传了 {@code null}」——后者是用户主动**清空**
 * （App 清空月供/还款日/总期数时会发 {@code null}）。实体绑定会把两者都变成 {@code null}，
 * 控制器因此改为显式映射白名单字段，并把 {@code body.keySet()}（显式出现的字段名）
 * 透传给 service，由 service 决定该列是否写入。</p>
 *
 * <p>转换规则统一为：{@code null} 或空白字符串 → {@code null}（即"清空"），
 * 非法数字/日期由调用方或上层异常处理兜住。</p>
 *
 * @author moyun
 */
final class LedgerAccountFields {

    private LedgerAccountFields() {
    }

    /** 字符串字段；null / 空白 → null */
    static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    /** 整数字段；null / 空白 → null */
    static Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : Integer.valueOf(s);
    }

    /** 金额字段（元）；null / 空白 → null。用字符串构造 BigDecimal，避免 double 精度噪声 */
    static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : new BigDecimal(s);
    }

    /** 日期字段（yyyy-MM-dd）；null / 空白 → null */
    static LocalDate asLocalDate(Object value) {
        String s = asString(value);
        if (s == null) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd：" + s);
        }
    }
}
