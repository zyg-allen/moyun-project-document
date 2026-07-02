package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户简历查询参数
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserResumeQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关键词（标题） */
    private String keyword;

    /** 状态：draft/published/archived */
    private String status;
}
