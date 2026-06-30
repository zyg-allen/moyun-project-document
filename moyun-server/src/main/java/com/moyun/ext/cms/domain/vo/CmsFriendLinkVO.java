package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsFriendLinkVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String url;
    private String description;
    private String logo;
    private Integer sort;
    private String status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
