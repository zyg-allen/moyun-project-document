package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历优化-评分报告存档（v10.18 简历模块重构补丁·阶段五）
 * <p>
 * 每次评分（单独评分 / 优化后重新评分 / 模板套用评分）结果存档，可追溯历史评分。
 * 与 portal_resume_optimize_history 区别：本表聚焦"评分快照"，
 * optimize_history 聚焦"优化采纳前后对比"（仅深度优化采纳时记录）。
 * </p>
 *
 * @author moyun
 */
@Data
@TableName("portal_resume_score_report")
public class PortalResumeScoreReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（门户用户ID） */
    private Long userId;

    /** 简历ID（portal_user_resume.id） */
    private Long resumeId;

    /** 关联岗位目标ID（可选，纯规则评分时为空） */
    private Long jobTargetId;

    /** 评分时的目标岗位快照（便于报告独立解读） */
    private String positionSnapshot;

    /** 综合评分 0-100 */
    private Integer score;

    /**
     * 各维度评分明细 JSON 字符串
     * 结构：基本信息/求职意向/教育/工作/项目/技能/自我评价/岗位匹配度 各项 score+max+subItems
     * 与 portal_user_resume.score_detail 字段格式一致，复用前端解析
     */
    private String scoreDetail;

    /**
     * 评分来源：manual 单独评分 / optimize 优化后重新评分 / template 模板套用评分
     */
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
