package com.moyun.ext.cms.domain.query;

import com.moyun.core.base.page.PageDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CmsNotificationQuery extends PageDomain implements Serializable {
    @Serial
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
