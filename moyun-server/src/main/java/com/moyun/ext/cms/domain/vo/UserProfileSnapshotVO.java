package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户画像快照 VO（v5.9 阶段0）
 * <p>
 * 抽题时基于此快照决策，并持久化到 portal_mock_interview.profile_snapshot 便于回溯。
 *
 * @author moyun
 */
@Data
public class UserProfileSnapshotVO {
    /** 用户ID */
    private Long userId;

    /** 目标岗位（来自前端选择或画像档案） */
    private String position;

    /** 面试场景（如 算法/系统设计） */
    private String scene;

    /** 岗位必备技能（来自 portal_interview_position.required_skills JSON 数组） */
    private List<String> requiredSkills;

    /** 薄弱知识点列表（按 failRate 降序） */
    private List<WeakTagItem> weakTags;

    /** 模拟面试次数（来自 portal_user_stats） */
    private Integer mockInterviewCount;

    /** 模拟面试平均分 */
    private Integer avgMockScore;

    /** 是否命中画像驱动（薄弱点 ≥ 1 或必备技能 ≥ 1） */
    private boolean personalized;

    /**
     * 薄弱知识点条目
     */
    @Data
    public static class WeakTagItem {
        /** 标签ID */
        private Long tagId;
        /** 标签名（如 Spring） */
        private String tagName;
        /** 该标签下用户答过的题目总数 */
        private Integer total;
        /** 通过数 */
        private Integer solved;
        /** 失败率 0.0-1.0（solved/total） */
        private Double failRate;
    }
}
