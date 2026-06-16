package com.moyun.ext.cms.domain.query;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.page.PageDomain;

/**
 * 门户用户查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CmsPortalUserQuery extends PageDomain implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 帐号状态（0正常 1停用）
     */
    private String status;

}
