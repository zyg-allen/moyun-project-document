package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_search_history")
public class CmsSearchHistory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long historyId;

    private Long userId;

    private String keyword;

    private Integer resultCount;
}