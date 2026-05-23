package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_search_hotword")
public class CmsSearchHotword extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long hotwordId;

    private String keyword;

    private Integer searchCount;

    private Integer sort;

    private String status;

    private String type;
}