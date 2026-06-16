package com.moyun.ext.cms.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.ext.cms.domain.query.CmsCommentQuery;
import com.moyun.ext.cms.domain.vo.CmsCommentVO;
import com.moyun.portal.domain.entity.PortalComment;

/**
 * CMS评论服务接口
 *
 * @author moyun
 */
public interface ICmsCommentService
{
    /**
     * 查询评论分页列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 评论分页列表
     */
    Page<CmsCommentVO> selectCommentPage(Page<CmsCommentVO> page, CmsCommentQuery query);

    /**
     * 查询评论列表
     *
     * @param query 查询条件
     * @return 评论列表
     */
    List<PortalComment> selectCommentList(CmsCommentQuery query);

    /**
     * 查询评论详情
     *
     * @param id 评论ID
     * @return 评论信息
     */
    PortalComment selectCommentById(Long id);

    /**
     * 审核评论
     *
     * @param comment 评论信息
     * @return 结果
     */
    int auditComment(PortalComment comment);

    /**
     * 批量删除评论
     *
     * @param ids 需要删除的评论ID
     * @return 结果
     */
    int deleteCommentByIds(Long[] ids);
}
