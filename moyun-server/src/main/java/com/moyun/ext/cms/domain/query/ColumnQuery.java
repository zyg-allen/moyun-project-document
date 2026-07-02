package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 专栏查询参数
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ColumnQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关键词（标题/副标题） */
    private String keyword;

    /** 状态：draft/published/archived */
    private String status;

    /** 分类ID */
    private Long categoryId;

    /** 排序方式：latest/popular/subscribe */
    private String sortBy;
}
