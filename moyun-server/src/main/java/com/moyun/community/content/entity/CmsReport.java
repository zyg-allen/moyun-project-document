package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_report")
public class CmsReport extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long reportId;

    private Long reportUserId;

    private String targetType;

    private Long targetId;

    private Long targetUserId;

    private String reason;

    private String content;

    private String evidenceUrls;

    private String status;

    private String handleResult;

    private Long handlerId;

    private String handleTime;
}