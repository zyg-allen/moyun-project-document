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

    /**
     * 是否只查"今日待复习"（**时间口径**）。
     *
     * <p>历史实现用 {@code status='reviewing'} 表达待复习，但全仓**没有任何写入点**会产生该状态
     * （只有 wrong → mastered），导致"今日待复习"恒为空。改为按 {@code next_review_time} 判定。</p>
     */
    private Boolean reviewOnly;

    /** 标签筛选（按题目标签模糊匹配） */
    private String tag;

    /** 关键词（题目标题模糊匹配） */
    private String keyword;
}
