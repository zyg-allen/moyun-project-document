package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int updateFeatured(@Param("id") Long id, @Param("isFeatured") boolean isFeatured);

    /**
     * 查询某题目的精选笔记列表
     *
     * @param questionId 题目ID
     * @return 精选提交记录列表
     */

    List<PortalInterviewSubmission> selectFeaturedByQuestion(@Param("questionId") Long questionId);

    /**
     * 查询用户所有提交记录（按提交时间倒序）
     *
     * @param userId 用户ID
     * @return 提交记录列表
     */

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

    List<Map<String, Object>> selectCalendarByUserAndYear(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 题目通过数排行榜 Top N（3.7 type=question）
     * <p>
     * 通过数 = 该用户去重通过的题目数；提交数 = 该用户全部提交数。
     * 返回每行 Map：{ user_id, nickname, avatar, passed_count, submit_count }
     *
     * @param limit 取前 N 名
     */

    List<Map<String, Object>> selectQuestionCountLeaderboard(@Param("limit") int limit);

    /**
     * 刷题积分排行榜 Top N（3.7 type=score）
     * <p>
     * 积分规则：通过 = 5 分，未通过 = 1 分。
     * 返回每行 Map：{ user_id, nickname, avatar, score, submit_count, passed_count }
     *
     * @param limit 取前 N 名
     */

    List<Map<String, Object>> selectScoreLeaderboard(@Param("limit") int limit);

    /**
     * 当前用户的通过题目数（3.7 我的排名 - question）
     */

    Long selectPassedQuestionCount(@Param("userId") Long userId);

    /**
     * 当前用户的刷题积分（3.7 我的排名 - score）
     */

    Long selectLearnScore(@Param("userId") Long userId);

    /**
     * 当前用户的提交总数（3.7 我的排名卡片）
     */

    Long selectSubmitCountByUser(@Param("userId") Long userId);

    /**
     * 当前用户的题目通过数排名（3.7 type=question，比该用户通过数更多的用户数 + 1）
     */

    Long selectQuestionCountRank(@Param("userId") Long userId);

    /**
     * 当前用户的刷题积分排名（3.7 type=score，比该用户积分更高的用户数 + 1）
     */

    Long selectLearnScoreRank(@Param("userId") Long userId);
}
