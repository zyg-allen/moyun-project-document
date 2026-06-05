package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.query.CommentQuery;
import com.moyun.portal.mapper.PortalCommentMapper;
import com.moyun.portal.service.IPortalCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户评论 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalCommentServiceImpl extends ServiceImpl<PortalCommentMapper, PortalComment> implements IPortalCommentService {

    @Autowired
    private PortalCommentMapper portalCommentMapper;

    /**
     * 根据条件分页查询评论列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalComment> selectPortalCommentPage(Page<PortalComment> page, CommentQuery query) {
        return baseMapper.selectPortalCommentPage(page, query);
    }

    /**
     * 根据条件查询评论列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 评论信息集合
     */
    @Override
    public List<PortalComment> selectPortalCommentList(CommentQuery query) {
        return baseMapper.selectPortalCommentList(query);
    }

    /**
     * 通过评论ID查询评论
     *
     * @param id 评论ID
     * @return 评论对象信息
     */
    @Override
    public PortalComment selectPortalCommentById(Long id) {
        return portalCommentMapper.selectPortalCommentById(id);
    }

    /**
     * 新增评论信息
     *
     * @param portalComment 评论信息
     * @return 结果
     */
    @Override
    public int insertPortalComment(PortalComment portalComment) {
        return portalCommentMapper.insertPortalComment(portalComment);
    }

    /**
     * 修改评论信息
     *
     * @param portalComment 评论信息
     * @return 结果
     */
    @Override
    public int updatePortalComment(PortalComment portalComment) {
        return portalCommentMapper.updatePortalComment(portalComment);
    }

    /**
     * 通过评论ID删除评论
     *
     * @param id 评论ID
     * @return 结果
     */
    @Override
    public int deletePortalCommentById(Long id) {
        return portalCommentMapper.deletePortalCommentById(id);
    }

    /**
     * 批量删除评论信息
     *
     * @param ids 需要删除的评论ID
     * @return 结果
     */
    @Override
    public int deletePortalCommentByIds(Long[] ids) {
        return portalCommentMapper.deletePortalCommentByIds(ids);
    }
}
