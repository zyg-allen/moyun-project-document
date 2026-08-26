package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历优化-优化历史（v10.13 简历优化重构）
 * <p>深度优化采纳后记录轨迹：优化前后简历版本、评分/匹配度对比、逐项采纳明细快照。</p>
 *
 * @author moyun
 */
@Data
@TableName(value = "portal_resume_optimize_history", autoResultMap = true)
public class PortalResumeOptimizeHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 优化后简历ID（新版本） */
    private Long resumeId;

    /** 优化前简历ID（旧版本） */
    private Long fromResumeId;

    /** 关联岗位目标ID */
    private Long jobTargetId;

    private Integer scoreBefore;

    private Integer scoreAfter;

    private Integer matchScoreBefore;

    private Integer matchScoreAfter;

    /** 采纳建议数 */
    private Integer adoptedCount;

    /** 生成建议总数 */
    private Integer totalCount;

    /** 优化明细快照 JSON（逐项 original/optimized/status） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private com.fasterxml.jackson.databind.JsonNode optimizeData;

    private LocalDateTime createTime;
}
