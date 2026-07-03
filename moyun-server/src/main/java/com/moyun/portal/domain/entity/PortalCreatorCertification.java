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
 * 创作者认证申请
 *
 * <p>状态机：pending（待审核）-> approved（已通过）/ rejected（已驳回）。
 * 同一用户存在 pending 申请时拒绝重复提交。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_creator_certification")
public class PortalCreatorCertification extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 认证类型 identity/creator/expert */
    private String certType;

    /** 证件号 */
    private String certNo;

    /** 证件照URL */
    private String certImage;

    /** 自我介绍 */
    private String intro;

    /** 代表作链接 */
    private String works;

    /** 审核状态 pending/approved/rejected */
    private String status;

    /** 审核人ID */
    private Long auditorId;

    /** 审核备注 */
    private String auditRemark;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditedTime;

    // BaseEntity 公共字段对应列在 portal_creator_certification 表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
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
}
