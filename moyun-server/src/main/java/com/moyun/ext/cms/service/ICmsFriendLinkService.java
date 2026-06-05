package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsFriendLinkQuery;
import com.moyun.ext.cms.domain.vo.CmsFriendLinkVO;
import com.moyun.portal.domain.entity.PortalFriendLink;

import java.util.List;

public interface ICmsFriendLinkService
{
    Page<CmsFriendLinkVO> selectFriendLinkPage(Page<CmsFriendLinkVO> page, CmsFriendLinkQuery query);
    List<CmsFriendLinkVO> selectFriendLinkList(CmsFriendLinkQuery query);
    CmsFriendLinkVO selectFriendLinkById(Long id);
    int insertFriendLink(PortalFriendLink friendLink);
    int updateFriendLink(PortalFriendLink friendLink);
    int deleteFriendLinkByIds(Long[] ids);
}
