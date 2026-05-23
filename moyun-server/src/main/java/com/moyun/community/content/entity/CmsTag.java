package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_tag")
public class CmsTag extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long tagId;

    private String tagName;

    private String tagColor;

    private Integer articleCount;

    private String status;
}