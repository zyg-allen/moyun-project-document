package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;

/**
 * 通知查询对象
 *
 * @author moyun
 */
@Data
public class CmsNotificationQuery extends PageDomain {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 类型
     */
    private String type;

    /**
     * 是否已读
     */
    private Boolean isRead;

}
