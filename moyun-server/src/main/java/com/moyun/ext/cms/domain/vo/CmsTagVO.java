package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 标签视图对象
 *
 * @author moyun
 */
@Data
public class CmsTagVO extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    private Long id;

    /** 标签名称 */
    private String name;

    /** 标签别名 */
    private String slug;

    /** 排序 */
    private Integer sort;

    /** 状态 */
    private String status;

    /** 文章数量 */
    private Integer articleCount;

    /** 备注 */
    private String remark;
}
