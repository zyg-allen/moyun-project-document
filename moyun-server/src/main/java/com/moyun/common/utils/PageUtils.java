package com.moyun.common.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.core.page.PageDomain;
import com.moyun.common.core.page.TableSupport;
import com.moyun.common.utils.sql.SqlUtil;

/**
 * 分页工具类
 * 
 * @author ruoyi
 */
public class PageUtils
{
    /**
     * 设置请求分页数据
     */
    public static <T> Page<T> startPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<T> page = new Page<>(pageNum, pageSize);
        
        // 设置排序
        if (StringUtils.isNotEmpty(pageDomain.getOrderByColumn())) {
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            // 提取排序字段和方向
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
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        // MyBatis-Plus 不需要手动清理线程变量
        // 此方法保留以保持兼容性
    }
}
