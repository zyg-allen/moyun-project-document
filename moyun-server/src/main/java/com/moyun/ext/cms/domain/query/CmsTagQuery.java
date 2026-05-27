package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CmsTagQuery extends PageDomain implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签别名
     */
    private String slug;

    /**
     * 状态
     */
    private String status;
}
