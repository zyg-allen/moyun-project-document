package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalHelpCategory;

import java.util.List;

/**
 * CMS 帮助分类管理 Service 接口
 *
 * @author moyun
 */
public interface ICmsHelpCategoryService {

    Page<PortalHelpCategory> selectHelpCategoryPage(Page<PortalHelpCategory> page, PortalHelpCategory category);

    List<PortalHelpCategory> selectHelpCategoryList(PortalHelpCategory category);

    PortalHelpCategory selectHelpCategoryById(Long id);

    int insertHelpCategory(PortalHelpCategory category);

    int updateHelpCategory(PortalHelpCategory category);

    int deleteHelpCategoryByIds(Long[] ids);
}
