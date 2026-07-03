package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalInterviewSubmission;

/**
 * 题目提交记录 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewSubmissionMapper extends BaseMapper<PortalInterviewSubmission>
{
    /**
     * 查询用户对某题目的最近提交
     */
    public List<PortalInterviewSubmission> selectSubmissionsByQuestionAndUser(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /**
     * 查询某题目的提交数量（用于计算通过率）
     */
    public long countSubmissionsByQuestion(@Param("questionId") Long questionId);

    /**
     * 查询某题目的通过数量（用于计算通过率）
     */
    public long countSuccessByQuestion(@Param("questionId") Long questionId);

    /**
     * 原子更新精选状态（后台采纳/取消采纳笔记）
     *
     * @param id           提交记录ID
     * @param isFeatured   是否精选
     * @return 受影响行数
     */
    @Update("UPDATE portal_interview_submission SET is_featured = #{isFeatured}, " +
            "featured_time = CASE WHEN #{isFeatured} = 1 THEN NOW() ELSE NULL END WHERE id = #{id}")
    int updateFeatured(@Param("id") Long id, @Param("isFeatured") boolean isFeatured);

    /**
     * 查询某题目的精选笔记列表
     *
     * @param questionId 题目ID
     * @return 精选提交记录列表
     */
    @Select("SELECT * FROM portal_interview_submission WHERE question_id = #{questionId} " +
            "AND is_featured = 1 AND note IS NOT NULL AND note != '' ORDER BY featured_time DESC")
    List<PortalInterviewSubmission> selectFeaturedByQuestion(@Param("questionId") Long questionId);

    /**
     * 查询用户所有提交记录（按提交时间倒序）
     *
     * @param userId 用户ID
     * @return 提交记录列表
     */
    @Select("SELECT * FROM portal_interview_submission WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<PortalInterviewSubmission> selectSubmissionsByUserId(@Param("userId") Long userId);

    // ==================== 学习统计（阶段三 3.4 / 3.7） ====================

    /**
     * 刷题日历：按日聚合某用户某年的提交数与通过数（3.4）
     * <p>
     * 返回每行 Map：{ date: "2026-01-01", count: 5, success_count: 3 }
     *
     * @param userId 门户用户ID
     * @param year   年份（如 2026）
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count, " +
            "SUM(CASE WHEN is_success = 1 THEN 1 ELSE 0 END) AS success_count " +
            "FROM portal_interview_submission " +
            "WHERE user_id = #{userId} AND YEAR(create_time) = #{year} " +
            "GROUP BY DATE(create_time) ORDER BY date ASC")
    List<Map<String, Object>> selectCalendarByUserAndYear(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 题目通过数排行榜 Top N（3.7 type=question）
     * <p>
     * 通过数 = 该用户去重通过的题目数；提交数 = 该用户全部提交数。
     * 返回每行 Map：{ user_id, nickname, avatar, passed_count, submit_count }
     *
     * @param limit 取前 N 名
     */
    @Select("SELECT s.user_id AS user_id, u.nickname AS nickname, u.avatar AS avatar, " +
            "COUNT(DISTINCT s.question_id) AS passed_count, " +
            "COUNT(*) AS submit_count " +
            "FROM portal_interview_submission s " +
            "LEFT JOIN portal_user u ON u.id = s.user_id " +
            "WHERE s.is_success = 1 " +
            "GROUP BY s.user_id, u.nickname, u.avatar " +
            "ORDER BY passed_count DESC, submit_count ASC, s.user_id ASC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectQuestionCountLeaderboard(@Param("limit") int limit);

    /**
     * 刷题积分排行榜 Top N（3.7 type=score）
     * <p>
     * 积分规则：通过 = 5 分，未通过 = 1 分。
     * 返回每行 Map：{ user_id, nickname, avatar, score, submit_count, passed_count }
     *
     * @param limit 取前 N 名
     */
    @Select("SELECT s.user_id AS user_id, u.nickname AS nickname, u.avatar AS avatar, " +
            "SUM(CASE WHEN s.is_success = 1 THEN 5 ELSE 1 END) AS score, " +
            "COUNT(*) AS submit_count, " +
            "SUM(CASE WHEN s.is_success = 1 THEN 1 ELSE 0 END) AS passed_count " +
            "FROM portal_interview_submission s " +
            "LEFT JOIN portal_user u ON u.id = s.user_id " +
            "GROUP BY s.user_id, u.nickname, u.avatar " +
            "ORDER BY score DESC, submit_count ASC, s.user_id ASC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectScoreLeaderboard(@Param("limit") int limit);

    /**
     * 当前用户的通过题目数（3.7 我的排名 - question）
     */
    @Select("SELECT COUNT(DISTINCT question_id) FROM portal_interview_submission " +
            "WHERE user_id = #{userId} AND is_success = 1")
    Long selectPassedQuestionCount(@Param("userId") Long userId);

    /**
     * 当前用户的刷题积分（3.7 我的排名 - score）
     */
    @Select("SELECT COALESCE(SUM(CASE WHEN is_success = 1 THEN 5 ELSE 1 END), 0) " +
            "FROM portal_interview_submission WHERE user_id = #{userId}")
    Long selectLearnScore(@Param("userId") Long userId);

    /**
     * 当前用户的提交总数（3.7 我的排名卡片）
     */
    @Select("SELECT COUNT(*) FROM portal_interview_submission WHERE user_id = #{userId}")
    Long selectSubmitCountByUser(@Param("userId") Long userId);

    /**
     * 当前用户的题目通过数排名（3.7 type=question，比该用户通过数更多的用户数 + 1）
     */
    @Select("SELECT COUNT(*) + 1 FROM (" +
            "  SELECT user_id FROM portal_interview_submission WHERE is_success = 1 " +
            "  GROUP BY user_id " +
            "  HAVING COUNT(DISTINCT question_id) > (" +
            "    SELECT COUNT(DISTINCT question_id) FROM portal_interview_submission " +
            "    WHERE user_id = #{userId} AND is_success = 1" +
            "  )" +
            ") t")
    Long selectQuestionCountRank(@Param("userId") Long userId);

    /**
     * 当前用户的刷题积分排名（3.7 type=score，比该用户积分更高的用户数 + 1）
     */
    @Select("SELECT COUNT(*) + 1 FROM (" +
            "  SELECT user_id FROM portal_interview_submission " +
            "  GROUP BY user_id " +
            "  HAVING SUM(CASE WHEN is_success = 1 THEN 5 ELSE 1 END) > (" +
            "    SELECT COALESCE(SUM(CASE WHEN is_success = 1 THEN 5 ELSE 1 END), 0) " +
            "    FROM portal_interview_submission WHERE user_id = #{userId}" +
            "  )" +
            ") t")
    Long selectLearnScoreRank(@Param("userId") Long userId);
}
