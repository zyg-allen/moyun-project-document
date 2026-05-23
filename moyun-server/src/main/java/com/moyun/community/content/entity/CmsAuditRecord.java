package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_audit_record")
public class CmsAuditRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long auditId;

    private Long articleId;

    private Long submitUserId;

    private Long auditorId;

    private String auditStatus;

    private String auditType;

    private String aiResult;

    private BigDecimal aiScore;

    private String auditReason;

    private String auditRemark;

    private Date submitTime;

    private Date auditTime;
}
