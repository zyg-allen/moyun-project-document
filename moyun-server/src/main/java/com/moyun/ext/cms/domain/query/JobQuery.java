package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 职位查询参数
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关键词（职位名称） */
    private String keyword;

    /** 公司ID */
    private Long companyId;

    /** 工作城市 */
    private String city;

    /** 经验要求 */
    private String experience;

    /** 学历要求 */
    private String education;

    /** 状态：open/closed */
    private String status;
}
