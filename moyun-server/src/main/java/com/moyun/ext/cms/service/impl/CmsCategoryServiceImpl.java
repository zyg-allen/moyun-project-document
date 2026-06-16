package com.moyun.ext.cms.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.ext.cms.domain.query.CmsCategoryQuery;
import com.moyun.ext.cms.domain.vo.CmsCategoryVO;
import com.moyun.ext.cms.service.ICmsCategoryService;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.mapper.PortalCategoryMapper;

/**
 * CMS分类服务实现类
 *
 * @author moyun
 */
@Service
public class CmsCategoryServiceImpl implements ICmsCategoryService
{
    @Autowired
    private PortalCategoryMapper portalCategoryMapper;

    @Override
    public List<CmsCategoryVO> selectCategoryList(CmsCategoryQuery query)
    {
        List<PortalCategory> list = portalCategoryMapper.selectList(buildQueryWrapper(query));
        return BeanUtil.copyToList(list, CmsCategoryVO.class);
    }

    @Override
    public List<CmsCategoryVO> selectCategoryTree(CmsCategoryQuery query)
    {
        List<PortalCategory> list = portalCategoryMapper.selectList(buildQueryWrapper(query));
        List<CmsCategoryVO> voList = BeanUtil.copyToList(list, CmsCategoryVO.class);
        return buildCategoryTree(voList);
    }

    @Override
    public PortalCategory selectCategoryById(Long id)
    {
        return portalCategoryMapper.selectById(id);
    }

    @Override
    public int insertCategory(PortalCategory category)
    {
        // 校验：不能超过两级栏目
        if (category.getParentId() != null && category.getParentId() != 0) {
            PortalCategory parent = portalCategoryMapper.selectById(category.getParentId());
            if (parent != null && parent.getParentId() != null && parent.getParentId() != 0) {
                throw new RuntimeException("最多只支持两级栏目，不能添加三级栏目");
            }
        }
        return portalCategoryMapper.insert(category);
    }

    @Override
    public int updateCategory(PortalCategory category)
    {
        // 校验：不能修改为三级栏目
        if (category.getParentId() != null && category.getParentId() != 0) {
            PortalCategory parent = portalCategoryMapper.selectById(category.getParentId());
            if (parent != null && parent.getParentId() != null && parent.getParentId() != 0) {
                throw new RuntimeException("最多只支持两级栏目，不能设置为三级");
            }
        }
        return portalCategoryMapper.updateById(category);
    }

    @Override
    public int deleteCategoryByIds(Long[] ids)
    {
        return portalCategoryMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public boolean hasChildCategory(Long id)
    {
        LambdaQueryWrapper<PortalCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalCategory::getParentId, id);
        return portalCategoryMapper.selectCount(wrapper) > 0;
    }

    @Override
    public int changeStatus(PortalCategory category)
    {
        PortalCategory updateCategory = new PortalCategory();
        updateCategory.setId(category.getId());
        updateCategory.setStatus(category.getStatus());
        return portalCategoryMapper.updateById(updateCategory);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalCategory> buildQueryWrapper(CmsCategoryQuery query)
    {
        LambdaQueryWrapper<PortalCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotEmpty(query.getName()), PortalCategory::getName, query.getName());
        wrapper.like(ObjectUtil.isNotEmpty(query.getSlug()), PortalCategory::getSlug, query.getSlug());
        wrapper.eq(ObjectUtil.isNotNull(query.getParentId()), PortalCategory::getParentId, query.getParentId());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalCategory::getStatus, query.getStatus());
        wrapper.orderByAsc(PortalCategory::getSort);
        return wrapper;
    }

    /**
     * 构建分类树
     */
    private List<CmsCategoryVO> buildCategoryTree(List<CmsCategoryVO> list)
    {
        List<CmsCategoryVO> tree = new ArrayList<>();
        for (CmsCategoryVO category : list)
        {
            if (category.getParentId() == null || category.getParentId() == 0L)
            {
                tree.add(category);
            }
        }
        for (CmsCategoryVO parent : tree)
        {
            List<CmsCategoryVO> children = list.stream()
                    .filter(c -> ObjectUtil.isNotNull(c.getParentId()) && c.getParentId().equals(parent.getId()))
                    .collect(Collectors.toList());
            parent.setChildren(children);
        }
        return tree;
    }
}
