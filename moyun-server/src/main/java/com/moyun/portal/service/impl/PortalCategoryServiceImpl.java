package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.mapper.PortalCategoryMapper;
import com.moyun.portal.service.IPortalCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户分类 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalCategoryServiceImpl extends ServiceImpl<PortalCategoryMapper, PortalCategory> implements IPortalCategoryService {

    @Autowired
    private PortalCategoryMapper portalCategoryMapper;

    /**
     * 根据条件分页查询分类列表
     *
     * @param portalCategory 分类信息
     * @return 分类信息集合信息
     */
    @Override
    public List<PortalCategory> selectPortalCategoryList(PortalCategory portalCategory) {
        return portalCategoryMapper.selectPortalCategoryList(portalCategory);
    }

    /**
     * 通过分类ID查询分类
     *
     * @param id 分类ID
     * @return 分类对象信息
     */
    @Override
    public PortalCategory selectPortalCategoryById(Long id) {
        return portalCategoryMapper.selectPortalCategoryById(id);
    }

    /**
     * 新增分类信息
     *
     * @param portalCategory 分类信息
     * @return 结果
     */
    @Override
    public int insertPortalCategory(PortalCategory portalCategory) {
        return portalCategoryMapper.insertPortalCategory(portalCategory);
    }

    /**
     * 修改分类信息
     *
     * @param portalCategory 分类信息
     * @return 结果
     */
    @Override
    public int updatePortalCategory(PortalCategory portalCategory) {
        return portalCategoryMapper.updatePortalCategory(portalCategory);
    }

    /**
     * 通过分类ID删除分类
     *
     * @param id 分类ID
     * @return 结果
     */
    @Override
    public int deletePortalCategoryById(Long id) {
        return portalCategoryMapper.deletePortalCategoryById(id);
    }

    /**
     * 批量删除分类信息
     *
     * @param ids 需要删除的分类ID
     * @return 结果
     */
    @Override
    public int deletePortalCategoryByIds(Long[] ids) {
        return portalCategoryMapper.deletePortalCategoryByIds(ids);
    }
}
