package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalTopicComment;

/**
 * 话题评论（多态） 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicCommentMapper extends BaseMapper<PortalTopicComment> {

    /**
     * 统计某目标下的一级评论数（parent_id=0 且未软删）
     */

    long countRootComments(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /**
     * 分页查询一级评论（按时间倒序，仅未软删）
     */

    List<PortalTopicComment> selectRoots(@Param("targetType") String targetType,
                                         @Param("targetId") Long targetId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    /**
     * 根据 rootId 列表查询每组前 10 条回复（按时间正序，楼中楼预加载）
     * 使用 MySQL 8 窗口函数 ROW_NUMBER() 在 SQL 层限制每组前 N 条，避免 Java 截取
     */

    List<PortalTopicComment> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

    /**
     * 软删单条评论
     */

    int softDelete(@Param("id") Long id);

    /**
     * 按 root_id 软删所有回复（级联软删某条一级评论下的所有回复）
     */

    int softDeleteByRoot(@Param("rootId") Long rootId);

    /**
     * 按话题 ID 级联软删该话题下所有一级评论（target_type='topic'）
     */

    int softDeleteByTopicId(@Param("topicId") Long topicId);

    /**
     * 按观点 ID 列表级联软删这些观点下的所有评论（target_type='post'）
     * 用于话题删除时同步软删其下观点的所有评论
     */

    int softDeleteByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 按观点 ID 级联软删该观点下的所有评论（target_type='post'）
     */

    int softDeleteByPostId(@Param("postId") Long postId);

    /**
     * 原子增加点赞数
     */

    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子增加 reply_count（仅一级评论维护）
     */

    int incrementReplyCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子减少 reply_count（仅一级评论维护）
     */

    int decrementReplyCount(@Param("id") Long id, @Param("delta") int delta);
}
