package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 面经评论查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterviewCommentQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long experienceId;        // 面经ID

    private Long userId;              // 作者用户ID

    private String status;            // 状态

    private String keyword;           // 内容模糊搜索
}
