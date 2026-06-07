package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.query.CategoryQuery;
import com.moyun.portal.mapper.PortalCategoryMapper;
import com.moyun.portal.service.IPortalCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalCategory> selectPortalCategoryPage(Page<PortalCategory> page, CategoryQuery query) {
        return baseMapper.selectPortalCategoryPage(page, query);
    }

    /**
     * 根据条件查询分类列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 分类信息集合
     */
    @Override
    public List<PortalCategory> selectPortalCategoryList(CategoryQuery query) {
        return baseMapper.selectPortalCategoryList(query);
    }

    /**
     * 获取树形分类列表
     *
     * @param query 查询条件
     * @return 树形分类列表
     */
    @Override
    public List<PortalCategory> selectPortalCategoryTree(CategoryQuery query) {
        List<PortalCategory> allCategories = baseMapper.selectPortalCategoryList(query);
        return buildTree(allCategories);
    }

    /**
     * 构建树形结构
     *
     * @param categories 分类列表
     * @return 树形结构
     */
    private List<PortalCategory> buildTree(List<PortalCategory> categories) {
        List<PortalCategory> rootNodes = categories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        for (PortalCategory root : rootNodes) {
            buildChildren(root, categories);
        }

        return rootNodes;
    }

    /**
     * 递归构建子节点
     *
     * @param parent 父节点
     * @param allCategories 所有分类列表
     */
    private void buildChildren(PortalCategory parent, List<PortalCategory> allCategories) {
        List<PortalCategory> children = allCategories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId().equals(parent.getId()))
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (PortalCategory child : children) {
                buildChildren(child, allCategories);
            }
        }
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

    /**
     * 获取分类的完整路径ID列表（从根到当前）
     *
     * @param categoryId 分类ID
     * @return 分类ID列表
     */
    @Override
    public List<Long> getCategoryPathIds(Long categoryId) {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        return portalCategoryMapper.selectCategoryAncestorIds(categoryId);
    }

    /**
     * 获取分类的路径字符串（逗号分隔）
     *
     * @param categoryId 分类ID
     * @return 路径字符串，例如：1,3,5
     */
    @Override
    public String getCategoryPath(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<Long> ids = getCategoryPathIds(categoryId);
        if (ids.isEmpty()) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 获取顶级分类ID
     *
     * @param categoryId 分类ID
     * @return 顶级分类ID
     */
    @Override
    public Long getRootCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<Long> ids = getCategoryPathIds(categoryId);
        if (ids.isEmpty()) {
            return categoryId;
        }
        return ids.get(0);
    }
}
