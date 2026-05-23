package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_banner")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "banner_id", type = IdType.AUTO)
    private Long bannerId;

    private String title;

    private String imageUrl;

    private String jumpType;

    private String jumpValue;

    private String jumpParams;

    private Integer sort;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String position;

    private Long clickCount;

    private Long exposureCount;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;
}
