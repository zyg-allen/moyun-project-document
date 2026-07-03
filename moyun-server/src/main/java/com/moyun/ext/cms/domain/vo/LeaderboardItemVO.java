package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 排行榜条目（3.7）
 *
 * @author moyun
 */
@Data
public class LeaderboardItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 名次（从 1 开始） */
    private Integer rank;

    /** 门户用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /**
     * 排行主指标值：
     * type=question 时为通过题目数；type=score 时为刷题积分。
     */
    private Integer value;

    /** 提交总数 */
    private Integer submitCount;

    /** 通过题目数 */
    private Integer passedCount;

    /** 刷题积分 */
    private Integer score;
}
