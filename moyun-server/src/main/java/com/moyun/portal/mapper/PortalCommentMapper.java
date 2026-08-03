package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 原子增加点赞数（避免并发丢失更新，参考 PortalArticleMapper.incrementLikes 模式）
     *
     * @param id    评论ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("UPDATE portal_comment SET like_count = like_count + #{delta} WHERE id = #{id} AND like_count + #{delta} >= 0")
    int incrementLikes(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 统计指定用户发表的评论收到的总点赞数
     * 用于成长统计 totalLikes 实时聚合（文章获赞 + 评论获赞），避免统计表不同步
     * 仅统计已发布评论（status = '1'）
     *
     * @param authorId 评论者用户ID
     * @return 评论获赞总数
     */
    @Select("SELECT coalesce(sum(like_count), 0) FROM portal_comment WHERE author_id = #{authorId} AND status = '1'")
    long sumCommentLikeReceived(@Param("authorId") Long authorId);

    /**
     * 批量统计多个用户发表的评论收到的总点赞数（避免 N+1 查询）
     * 仅统计已发布评论（status = '1'）
     *
     * @param authorIds 评论者用户ID集合
     * @return 每个用户一行，字段：userId / cnt
     */
    @Select("<script>" +
            "SELECT author_id AS userId, coalesce(sum(like_count), 0) AS cnt FROM portal_comment " +
            "WHERE status = '1' AND author_id IN " +
            "<foreach item='id' collection='authorIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY author_id" +
            "</script>")
    List<Map<String, Object>> batchSumCommentLikeReceived(@Param("authorIds") List<Long> authorIds);
}
