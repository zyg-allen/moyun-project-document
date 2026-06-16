package com.moyun.ext.cms.domain.query;

import lombok.Data;

import com.moyun.core.base.page.PageDomain;

@Data
public class CmsFriendLinkQuery extends PageDomain {
    private String name;
    private String status;

}
