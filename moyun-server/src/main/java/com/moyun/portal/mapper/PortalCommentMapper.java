package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalComment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
     * @param portalComment 评论信息
     * @return 评论信息集合信息
     */
    public List<PortalComment> selectPortalCommentList(PortalComment portalComment);

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
