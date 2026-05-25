package com.moyun.util.string;

import com.moyun.common.exception.UtilException;

public class SqlUtil {
    public static String SQL_REGEX = "\u000B|and |extractvalue|updatexml|sleep|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |union |like |+|/*|user()";

    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    private static final int ORDER_BY_MAX_LENGTH = 500;

    public static String escapeOrderBySql(String value) {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value)) {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH) {
            throw new UtilException("参数已超过最大限制，不能进行查询");
        }
        return value;
    }

    public static boolean isValidOrderBySql(String value) {
        return value.matches(SQL_PATTERN);
    }

    public static void filterKeyword(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords) {
            if (StringUtils.indexOfIgnoreCase(value, sqlKeyword) > -1) {
                throw new UtilException("参数存在SQL注入风险");
            }
        }
    }
}
