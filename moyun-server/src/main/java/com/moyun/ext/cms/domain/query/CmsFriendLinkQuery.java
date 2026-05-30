package com.moyun.ext.cms.domain.query;


import com.moyun.core.base.page.PageDomain;
import lombok.Data;

@Data
public class CmsFriendLinkQuery extends PageDomain {
    private String name;
    private String status;

}
