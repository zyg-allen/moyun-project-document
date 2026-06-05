package com.moyun.core.base.page;

import com.moyun.core.base.text.Convert;
import com.moyun.util.string.StringUtils;

/**
 * 表格数据处理
 *
 * @author moyun
 */
public class TableSupport {
    /**
     * 当前记录起始索引（兼容 page 和 pageNum）
     */
    public static final String PAGE_NUM = "pageNum";
    public static final String PAGE_NUM_ALIAS = "page";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 封装分页对象
     */
    public static PageDomain buildPageRequest() {
        PageDomain pageDomain = new PageDomain();
        // 优先使用 pageNum，若未传则使用 page（兼容前端）
        String pageNumValue = getParameter(PAGE_NUM);
        if (StringUtils.isEmpty(pageNumValue)) {
            pageNumValue = getParameter(PAGE_NUM_ALIAS);
        }
        pageDomain.setPageNum(Convert.toInt(pageNumValue));
        pageDomain.setPageSize(Convert.toInt(getParameter(PAGE_SIZE)));
        pageDomain.setOrderByColumn(getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(getParameter(IS_ASC));
        return pageDomain;
    }

    /**
     * 获取分页参数
     */
    public static PageDomain buildPageRequest(PageDomain pageDomain) {
        if (pageDomain == null) {
            pageDomain = new PageDomain();
        }
        // 优先使用 pageNum，若未传则使用 page（兼容前端）
        if (pageDomain.getPageNum() == null || pageDomain.getPageNum() <= 0) {
            String pageNumValue = getParameter(PAGE_NUM);
            if (StringUtils.isEmpty(pageNumValue)) {
                pageNumValue = getParameter(PAGE_NUM_ALIAS);
            }
            pageDomain.setPageNum(Convert.toInt(pageNumValue, 1));
        }
        if (pageDomain.getPageSize() == null || pageDomain.getPageSize() <= 0) {
            pageDomain.setPageSize(Convert.toInt(getParameter(PAGE_SIZE), 10));
        }
        if (StringUtils.isEmpty(pageDomain.getOrderByColumn())) {
            pageDomain.setOrderByColumn(getParameter(ORDER_BY_COLUMN));
        }
        if (StringUtils.isEmpty(pageDomain.getIsAsc())) {
            pageDomain.setIsAsc(getParameter(IS_ASC));
        }
        return pageDomain;
    }

    /**
     * 从request获取参数
     *
     * @param name 参数名
     * @return 参数值
     */
    public static String getParameter(String name) {
        return com.moyun.util.http.ServletUtils.getParameter(name);
    }

    /**
     * 检查SQL注入
     *
     * @param value 参数值
     * @return 是否包含SQL注入
     */
    public static boolean checkSqlInjection(String value) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        // 非法字符
        String[] keywords = {"master", "truncate", "insert", "select", "delete", "update", "declare", "exec", "execute", "xp_", "sp_", "0x"};
        value = value.toLowerCase();
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建排序
     */
    public static String buildOrderBySql(PageDomain pageDomain) {
        if (pageDomain == null || StringUtils.isEmpty(pageDomain.getOrderByColumn())) {
            return "";
        }
        // 检查SQL注入
        if (checkSqlInjection(pageDomain.getOrderByColumn())) {
            return "";
        }
        // 防止SQL注入
        String orderBy = com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(pageDomain.getOrderByColumn());
        // 拼接排序语句
        return orderBy + " " + pageDomain.getIsAsc();
    }

    /**
     * 构建排序（兼容JPA）
     */
    public static String buildOrderBy(PageDomain pageDomain) {
        if (pageDomain == null || StringUtils.isEmpty(pageDomain.getOrderByColumn())) {
            return "";
        }
        // 检查SQL注入
        if (checkSqlInjection(pageDomain.getOrderByColumn())) {
            return "";
        }
        // 拼接排序语句
        return pageDomain.getOrderByColumn() + " " + pageDomain.getIsAsc();
    }
}
