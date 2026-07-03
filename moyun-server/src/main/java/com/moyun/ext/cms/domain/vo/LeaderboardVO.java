package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 排行榜聚合数据（3.7）
 *
 * @author moyun
 */
@Data
public class LeaderboardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 排行类型：question / score */
    private String type;

    /** Top N 列表 */
    private List<LeaderboardItemVO> list;

    /** 当前用户名次（未登录或无提交记录时为 null） */
    private Integer myRank;

    /** 当前用户主指标值（未登录时为 null） */
    private Integer myValue;

    /** 当前用户提交总数（未登录时为 null） */
    private Integer mySubmitCount;

    /** 当前用户通过题目数（未登录时为 null） */
    private Integer myPassedCount;

    /** 当前用户刷题积分（未登录时为 null） */
    private Integer myScore;
}
