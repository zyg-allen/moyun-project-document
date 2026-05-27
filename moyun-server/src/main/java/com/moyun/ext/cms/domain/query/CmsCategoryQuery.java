package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分类查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CmsCategoryQuery extends PageDomain {
    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类别名
     */
    private String slug;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

}
