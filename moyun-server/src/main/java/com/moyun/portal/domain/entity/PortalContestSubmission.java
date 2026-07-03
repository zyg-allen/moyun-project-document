package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动投稿
 *
 * @author moyun
 */
@Data
@TableName("portal_contest_submission")
public class PortalContestSubmission extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动ID */
    private Long contestId;

    /** 投稿用户ID */
    private Long userId;

    /** 投稿文章ID */
    private Long articleId;

    /** 状态 pending/shortlisted/eliminated/winner */
    private String status;

    /** 投票数 */
    private Integer voteCount;

    /** 排名 */
    private Integer rank;

    /** 备注（评审意见等） */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // BaseEntity 公共字段对应列在表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
}
