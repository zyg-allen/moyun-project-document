package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 专栏文章排序项（批量排序请求体）
 *
 * @author moyun
 */
@Data
public class ColumnArticleSortItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private Long id;

    /** 专栏内顺序 */
    private Integer sortOrder;
}
