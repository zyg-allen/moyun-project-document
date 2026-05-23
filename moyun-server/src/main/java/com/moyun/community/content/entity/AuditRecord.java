package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_audit_record")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "audit_id", type = IdType.AUTO)
    private Long auditId;

    private Long articleId;

    private Long submitUserId;

    private Long auditorId;

    private String auditStatus;

    private String auditType;

    private String aiResult;

    private Double aiScore;

    private String auditReason;

    private String auditRemark;

    private LocalDateTime submitTime;

    private LocalDateTime auditTime;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
