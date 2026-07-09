package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成长时间线 VO
 * 统一展示用户的读书、面试、创作等各模块行为记录
 * 数据来源：portal_growth_log（行为流水表）
 */
@Data
@Schema(description = "成长时间线VO")
public class GrowthTimelineVO implements Serializable {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "模块：article/reading/interview/all")
    private String module;

    @Schema(description = "行为编码：publish_article/write_quote/solve_question 等")
    private String action;

    @Schema(description = "实体类型：article/book/question/quote/booklist/experience 等")
    private String entityType;

    @Schema(description = "实体ID")
    private Long entityId;

    @Schema(description = "成长值变化")
    private Integer growthDelta;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // ===== 关联展示字段（按 entityType 动态填充） =====

    @Schema(description = "目标标题（文章标题/书名/题目标题等）")
    private String targetTitle;

    @Schema(description = "目标封面图")
    private String targetCover;

    @Schema(description = "前端跳转路径")
    private String targetUrl;

    @Schema(description = "模块图标标识，用于前端渲染图标")
    private String icon;

    @Schema(description = "行为标签，如\"发布文章\"、\"记录金句\"、\"答对题目\"")
    private String actionLabel;
}
