package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.page.PageDomain;

/**
 * 评论查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CmsCommentQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 评论者ID
     */
    private Long authorId;

    /**
     * 状态
     */
    private String status;

}
