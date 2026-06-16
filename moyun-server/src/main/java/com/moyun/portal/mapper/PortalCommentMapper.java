package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.domain.query.CommentQuery;

/**
 * 门户评论表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalCommentMapper extends BaseMapper<PortalComment> {

    /**
     * 根据条件分页查询评论列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 评论信息集合信息
     */
    Page<PortalComment> selectPortalCommentPage(Page<PortalComment> page, @Param("params") CommentQuery query);

    /**
     * 根据条件查询评论列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 评论信息集合信息
     */
    List<PortalComment> selectPortalCommentList(@Param("params") CommentQuery query);

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
