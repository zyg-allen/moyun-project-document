package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalUserStats;

/**
 * 门户用户统计聚合 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalUserStatsMapper extends BaseMapper<PortalUserStats> {

    /**
     * 根据用户ID查询统计
     */

    PortalUserStats selectByUserId(@Param("userId") Long userId);

    /**
     * 插入（如果不存在）
     */

    int insertIfNotExists(@Param("userId") Long userId);

    /**
     * 原子增加文章数
     */

    int addArticleCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加文章浏览量
     */

    int addArticleViewSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加文章获赞数
     */

    int addArticleLikeSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加文章收藏数
     */

    int addArticleBookmarkSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加创作字数
     */

    int addArticleWordSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加读完的书
     */

    int addBookFinished(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加书单数
     */

    int addBooklistCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加金句数
     */

    int addQuoteCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加阅读时长
     */

    int addReadingMinutes(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加解题数
     */

    int addQuestionSolved(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加笔记数
     */

    int addNoteCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加面经数
     */

    int addExperienceCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加笔记被精选数
     */

    int addNoteAdopted(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加粉丝数
     */

    int addFollowerCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加关注数
     */

    int addFollowingCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加评论数
     */

    int addCommentCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加总获赞数（跨模块）
     */

    int addTotalLikeReceived(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 阶段0：更新薄弱知识点 JSON 与计算时间
     */

    int updateWeakTags(@Param("userId") Long userId, @Param("weakTags") String weakTags);
}
