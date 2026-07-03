package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 错题本查询参数
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WrongQuestionQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 状态筛选：wrong/reviewing/mastered（不传则查全部） */
    private String status;

    /** 标签筛选（按题目标签模糊匹配） */
    private String tag;

    /** 关键词（题目标题模糊匹配） */
    private String keyword;
}
