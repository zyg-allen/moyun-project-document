package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    @Select("SELECT * FROM portal_user_stats WHERE user_id = #{userId}")
    PortalUserStats selectByUserId(@Param("userId") Long userId);

    /**
     * 插入（如果不存在）
     */
    @Update("INSERT IGNORE INTO portal_user_stats (user_id) VALUES (#{userId})")
    int insertIfNotExists(@Param("userId") Long userId);

    /**
     * 原子增加文章数
     */
    @Update("UPDATE portal_user_stats SET article_count = article_count + #{delta} WHERE user_id = #{userId}")
    int addArticleCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加文章浏览量
     */
    @Update("UPDATE portal_user_stats SET article_view_sum = article_view_sum + #{delta} WHERE user_id = #{userId}")
    int addArticleViewSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加文章获赞数
     */
    @Update("UPDATE portal_user_stats SET article_like_sum = article_like_sum + #{delta}, total_like_received = total_like_received + #{delta} WHERE user_id = #{userId}")
    int addArticleLikeSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加文章收藏数
     */
    @Update("UPDATE portal_user_stats SET article_bookmark_sum = article_bookmark_sum + #{delta} WHERE user_id = #{userId}")
    int addArticleBookmarkSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加创作字数
     */
    @Update("UPDATE portal_user_stats SET article_word_sum = article_word_sum + #{delta} WHERE user_id = #{userId}")
    int addArticleWordSum(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加读完的书
     */
    @Update("UPDATE portal_user_stats SET book_finished = book_finished + #{delta} WHERE user_id = #{userId}")
    int addBookFinished(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加书单数
     */
    @Update("UPDATE portal_user_stats SET booklist_count = booklist_count + #{delta} WHERE user_id = #{userId}")
    int addBooklistCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加金句数
     */
    @Update("UPDATE portal_user_stats SET quote_count = quote_count + #{delta} WHERE user_id = #{userId}")
    int addQuoteCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加阅读时长
     */
    @Update("UPDATE portal_user_stats SET reading_minutes = reading_minutes + #{delta} WHERE user_id = #{userId}")
    int addReadingMinutes(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 原子增加解题数
     */
    @Update("UPDATE portal_user_stats SET question_solved = question_solved + #{delta} WHERE user_id = #{userId}")
    int addQuestionSolved(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加笔记数
     */
    @Update("UPDATE portal_user_stats SET note_count = note_count + #{delta} WHERE user_id = #{userId}")
    int addNoteCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加面经数
     */
    @Update("UPDATE portal_user_stats SET experience_count = experience_count + #{delta} WHERE user_id = #{userId}")
    int addExperienceCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加笔记被精选数
     */
    @Update("UPDATE portal_user_stats SET note_adopted = note_adopted + #{delta} WHERE user_id = #{userId}")
    int addNoteAdopted(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加粉丝数
     */
    @Update("UPDATE portal_user_stats SET follower_count = follower_count + #{delta} WHERE user_id = #{userId}")
    int addFollowerCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加关注数
     */
    @Update("UPDATE portal_user_stats SET following_count = following_count + #{delta} WHERE user_id = #{userId}")
    int addFollowingCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加评论数
     */
    @Update("UPDATE portal_user_stats SET comment_count = comment_count + #{delta} WHERE user_id = #{userId}")
    int addCommentCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子增加总获赞数（跨模块）
     */
    @Update("UPDATE portal_user_stats SET total_like_received = total_like_received + #{delta} WHERE user_id = #{userId}")
    int addTotalLikeReceived(@Param("userId") Long userId, @Param("delta") long delta);
}
