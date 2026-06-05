package com.moyun.util.bean;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.core.base.page.TableSupport;
import com.moyun.util.string.SqlUtil;
import com.moyun.util.string.StringUtils;

/**
 * 分页工具类
 *
 * @author moyun
 */
public class PageUtils {

    /** 默认页码 */
    private static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页数量 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 最大每页数量 */
    private static final int MAX_PAGE_SIZE = 100;


    public static <T> Page<T> startPage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<T> page = new Page<>(pageNum, pageSize);

        if (com.moyun.util.string.StringUtils.isNotEmpty(pageDomain.getOrderByColumn())) {
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            String[] orderParts = orderBy.split("\\s+");
            if (orderParts.length >= 2) {
                String orderField = orderParts[0];
                String orderDirection = orderParts[1].equalsIgnoreCase("asc") ? "asc" : "desc";
                if ("asc".equals(orderDirection)) {
                    page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.asc(orderField));
                } else {
                    page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc(orderField));
                }
            }

        }
         return page;
    }

    /**
     * 构建分页对象
     *
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return Page对象
     */
    public static <T> Page<T> buildPage(Integer pageNum, Integer pageSize) {
        return buildPage(pageNum, pageSize, null, null);
    }

    /**
     * 构建分页对象（带排序）
     *
     * @param pageNum       页码
     * @param pageSize      每页数量
     * @param orderByColumn 排序列
     * @param isAsc         排序方向（asc/desc）
     * @return Page对象
     */
    public static <T> Page<T> buildPage(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc) {
        Page<T> page = new Page<>(
            pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum,
            safePageSize(pageSize)
        );

        // 设置排序
        if (StringUtils.isNotEmpty(orderByColumn)) {
            // 驼峰转下划线
            String column = toUnderScoreCase(orderByColumn);
            boolean asc = !"desc".equalsIgnoreCase(isAsc);
            page.addOrder(asc ? OrderItem.asc(column) : OrderItem.desc(column));
        }

        return page;
    }

    /**
     * 从 Query 对象构建分页对象（Query需继承 PageDomain）
     *
     * @param query 查询对象
     * @return Page对象
     */
    public static <T> Page<T> buildPage(Object query) {
        if (query == null) {
            return new Page<>(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE);
        }

        // 继承 PageDomain 的 Query
        if (query instanceof PageDomain pageDomain) {
            return buildPage(
                pageDomain.getPageNum(),
                pageDomain.getPageSize(),
                pageDomain.getOrderByColumn(),
                pageDomain.getIsAsc()
            );
        }

        return new Page<>(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE);
    }

    /**
     * 安全的每页数量处理
     */
    private static int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 驼峰转下划线
     */
    private static String toUnderScoreCase(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void clearPage()
    {
        // MyBatis-Plus 不需要手动清理线程变量
        // 此方法保留以保持兼容性
    }

}
