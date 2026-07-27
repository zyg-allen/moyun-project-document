package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PK 对战（异步对战，3.7 排行榜 / PK）
 *
 * @author moyun
 */
@Data
@TableName("portal_pk_challenge")
public class PortalPkChallenge extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起方用户ID */
    private Long challengerId;

    /** 应战方用户ID */
    private Long opponentId;

    /** 状态:pending/accepted/declined/ongoing/finished */
    private String status;

    /** 胜者用户ID（平局为 null） */
    private Long winnerId;

    /** 发起方得分（通过题数） */
    private Integer challengerScore;

    /** 应战方得分（通过题数） */
    private Integer opponentScore;

    /** 题目ID列表，逗号分隔 */
    private String questionIds;

    /** 场景:1v1=好友PK / company=公司题目挑战 */
    private String scene;

    /** 公司ID（scene=company 时关联 portal_interview_company） */
    private Long companyId;

    /** 发起时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    // ===== 仅用于详情/列表展示的扩展字段（不持久化） =====

    /** 发起方昵称 */
    @TableField(exist = false)
    private String challengerNickname;

    /** 发起方头像 */
    @TableField(exist = false)
    private String challengerAvatar;

    /** 应战方昵称 */
    @TableField(exist = false)
    private String opponentNickname;

    /** 应战方头像 */
    @TableField(exist = false)
    private String opponentAvatar;

    /** 题目简要列表（id / title / difficulty） */
    @TableField(exist = false)
    private List<Map<String, Object>> questions;

    // BaseEntity 公共字段对应列在 portal_pk_challenge 表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;

    // 覆盖 BaseEntity 的 delFlag：本表无 del_flag 列（迁移脚本排除），保持物理删除（toggle/流水语义）
    @TableField(exist = false)
    private String delFlag;
}
