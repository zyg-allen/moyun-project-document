package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 圈子查询参数
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CircleQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关键词（名称/简介） */
    private String keyword;

    /** 状态：active/disabled/pending */
    private String status;

    /** 分类：reading/writing/tech */
    private String category;

    /** 排序方式：latest/popular/members */
    private String sortBy;
}
