package com.moyun.ext.cms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsCommentQuery;
import com.moyun.ext.cms.domain.vo.CmsCommentVO;
import com.moyun.ext.cms.service.ICmsCommentService;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.mapper.PortalCommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * CMS评论服务实现类
 *
 * @author moyun
 */
@Service
public class CmsCommentServiceImpl implements ICmsCommentService
{
    @Autowired
    private PortalCommentMapper portalCommentMapper;

    @Override
    public Page<CmsCommentVO> selectCommentPage(Page<CmsCommentVO> page, CmsCommentQuery query)
    {
        Page<PortalComment> entityPage = new Page<>(page.getCurrent(), page.getSize());
        entityPage = portalCommentMapper.selectPage(entityPage, buildQueryWrapper(query));
        
        Page<CmsCommentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CmsCommentVO> voList = BeanUtil.copyToList(entityPage.getRecords(), CmsCommentVO.class);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<PortalComment> selectCommentList(CmsCommentQuery query)
    {
        return portalCommentMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalComment selectCommentById(Long id)
    {
        return portalCommentMapper.selectById(id);
    }

    @Override
    public int auditComment(PortalComment comment)
    {
        PortalComment updateComment = new PortalComment();
        updateComment.setId(comment.getId());
        updateComment.setStatus(comment.getStatus());
        return portalCommentMapper.updateById(updateComment);
    }

    @Override
    public int deleteCommentByIds(Long[] ids)
    {
        return portalCommentMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalComment> buildQueryWrapper(CmsCommentQuery query)
    {
        LambdaQueryWrapper<PortalComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtil.isNotNull(query.getArticleId()), PortalComment::getArticleId, query.getArticleId());
        wrapper.eq(ObjectUtil.isNotNull(query.getAuthorId()), PortalComment::getAuthorId, query.getAuthorId());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalComment::getStatus, query.getStatus());
        wrapper.orderByDesc(PortalComment::getCreateTime);
        return wrapper;
    }
}
