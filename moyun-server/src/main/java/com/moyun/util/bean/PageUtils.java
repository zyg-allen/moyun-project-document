package com.moyun.util.bean;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.core.base.page.TableSupport;
import com.moyun.util.string.SqlUtil;

public class PageUtils
{
    public static <T> Page<T> startPage()
    {
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

    public static void clearPage()
    {
    }
}
