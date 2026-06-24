package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.service.ICmsHelpCategoryService;
import com.moyun.portal.domain.entity.PortalHelpCategory;
import com.moyun.portal.mapper.PortalHelpCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CMS 帮助分类管理 Service 实现
 *
 * @author moyun
 */
@Service
public class CmsHelpCategoryServiceImpl implements ICmsHelpCategoryService {

    @Autowired
    private PortalHelpCategoryMapper helpCategoryMapper;

    @Override
    public Page<PortalHelpCategory> selectHelpCategoryPage(Page<PortalHelpCategory> page, PortalHelpCategory category) {
        return helpCategoryMapper.selectHelpCategoryPage(page, category);
    }

    @Override
    public List<PortalHelpCategory> selectHelpCategoryList(PortalHelpCategory category) {
        return helpCategoryMapper.selectHelpCategoryList(category);
    }

    @Override
    public PortalHelpCategory selectHelpCategoryById(Long id) {
        return helpCategoryMapper.selectById(id);
    }

    @Override
    public int insertHelpCategory(PortalHelpCategory category) {
        return helpCategoryMapper.insert(category);
    }

    @Override
    public int updateHelpCategory(PortalHelpCategory category) {
        return helpCategoryMapper.updateById(category);
    }

    @Override
    public int deleteHelpCategoryByIds(Long[] ids) {
        return helpCategoryMapper.deleteBatchIds(java.util.Arrays.asList(ids));
    }
}
