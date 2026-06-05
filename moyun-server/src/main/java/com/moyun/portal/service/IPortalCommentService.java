package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.query.CommentQuery;

import java.util.List;

/**
 * 门户评论 业务层
 *
 * @author moyun
 */
public interface IPortalCommentService {

    /**
     * 根据条件分页查询评论列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalComment> selectPortalCommentPage(Page<PortalComment> page, CommentQuery query);

    /**
     * 根据条件查询评论列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 评论信息集合
     */
    List<PortalComment> selectPortalCommentList(CommentQuery query);

    /**
     * 通过评论ID查询评论
     *
     * @param id 评论ID
     * @return 评论对象信息
     */
    public PortalComment selectPortalCommentById(Long id);

    /**
     * 新增评论信息
     *
     * @param portalComment 评论信息
     * @return 结果
     */
    public int insertPortalComment(PortalComment portalComment);

    /**
     * 修改评论信息
     *
     * @param portalComment 评论信息
     * @return 结果
     */
    public int updatePortalComment(PortalComment portalComment);

    /**
     * 通过评论ID删除评论
     *
     * @param id 评论ID
     * @return 结果
     */
    public int deletePortalCommentById(Long id);

    /**
     * 批量删除评论信息
     *
     * @param ids 需要删除的评论ID
     * @return 结果
     */
    public int deletePortalCommentByIds(Long[] ids);
}
