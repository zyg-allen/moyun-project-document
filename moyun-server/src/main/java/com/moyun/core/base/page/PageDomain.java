package com.moyun.core.base.page;


import com.moyun.core.base.BaseEntity;
import com.moyun.util.string.StringUtils;

/**
 * 分页数据
 * <p>
 * 继承 BaseEntity 以获得 params Map，使前端 addDateRange 塞入的
 * params.beginTime / params.endTime 能正常被 Spring 绑定与 OGNL 解析，
 * 修复登录日志/操作日志等列表的时间范围筛选失效问题。
 *
 * @author ruoyi
 */
public class PageDomain extends BaseEntity {
    /**
     * 当前记录起始索引
     */
    private Integer pageNum = 1;

    /**
     * 每页显示记录数
     */
    private Integer pageSize = 10;

    /**
     * 排序列
     */
    private String orderByColumn;

    /**
     * 排序的方向desc或者asc
     */
    private String isAsc = "asc";

    /**
     * 分页参数合理化
     */
    private Boolean reasonable = true;

    public String getOrderBy() {
        if (StringUtils.isEmpty(orderByColumn)) {
            return "";
        }
        return StringUtils.toUnderScoreCase(orderByColumn) + " " + isAsc;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderByColumn() {
        return orderByColumn;
    }

    public void setOrderByColumn(String orderByColumn) {
        if (StringUtils.isEmpty(orderByColumn)) {
            this.orderByColumn = null;
            return;
        }
        if (!orderByColumn.matches("^[a-zA-Z][a-zA-Z0-9_]{0,63}$")) {
            throw new IllegalArgumentException("排序列名不合法：" + orderByColumn);
        }
        this.orderByColumn = orderByColumn;
    }

    public String getIsAsc() {
        return isAsc;
    }

    public void setIsAsc(String isAsc) {
        if (StringUtils.isNotEmpty(isAsc)) {
            if ("ascending".equals(isAsc)) {
                isAsc = "asc";
            } else if ("descending".equals(isAsc)) {
                isAsc = "desc";
            }
            if (!"asc".equals(isAsc) && !"desc".equals(isAsc)) {
                throw new IllegalArgumentException("排序方向不合法：" + isAsc);
            }
            this.isAsc = isAsc;
        }
    }

    public Boolean getReasonable() {
        if (StringUtils.isNull(reasonable)) {
            return Boolean.TRUE;
        }
        return reasonable;
    }

    public void setReasonable(Boolean reasonable) {
        this.reasonable = reasonable;
    }
}
