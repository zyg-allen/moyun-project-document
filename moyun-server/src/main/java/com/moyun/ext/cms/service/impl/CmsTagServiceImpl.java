package com.moyun.ext.cms.service.impl;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.ext.cms.domain.query.CmsTagQuery;
import com.moyun.ext.cms.domain.vo.CmsTagVO;
import com.moyun.ext.cms.service.ICmsTagService;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.mapper.PortalTagMapper;

/**
 * CMS标签服务实现类
 *
 * @author moyun
 */
@Service
public class CmsTagServiceImpl implements ICmsTagService
{
    @Autowired
    private PortalTagMapper portalTagMapper;

    @Override
    public Page<CmsTagVO> selectTagPage(Page<CmsTagVO> page, CmsTagQuery query)
    {
        Page<PortalTag> entityPage = new Page<>(page.getCurrent(), page.getSize());
        entityPage = portalTagMapper.selectPage(entityPage, buildQueryWrapper(query));
        
        Page<CmsTagVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CmsTagVO> voList = BeanUtil.copyToList(entityPage.getRecords(), CmsTagVO.class);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<PortalTag> selectTagList(CmsTagQuery query)
    {
        return portalTagMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalTag selectTagById(Long id)
    {
        return portalTagMapper.selectById(id);
    }

    @Override
    public int insertTag(PortalTag tag)
    {
        return portalTagMapper.insert(tag);
    }

    @Override
    public int updateTag(PortalTag tag)
    {
        return portalTagMapper.updateById(tag);
    }

    @Override
    public int deleteTagByIds(Long[] ids)
    {
        return portalTagMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalTag> buildQueryWrapper(CmsTagQuery query)
    {
        LambdaQueryWrapper<PortalTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotEmpty(query.getName()), PortalTag::getName, query.getName());
        wrapper.like(ObjectUtil.isNotEmpty(query.getSlug()), PortalTag::getSlug, query.getSlug());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalTag::getStatus, query.getStatus());
        wrapper.orderByAsc(PortalTag::getSort);
        return wrapper;
    }
}
