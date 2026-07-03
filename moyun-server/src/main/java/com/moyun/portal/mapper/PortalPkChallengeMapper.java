package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalPkChallenge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * PK 对战 Mapper（3.7 排行榜 / PK）
 *
 * @author moyun
 */
@Mapper
public interface PortalPkChallengeMapper extends BaseMapper<PortalPkChallenge> {

    /**
     * 统计某用户在某场对战中的答题数（不同 question_id，已尝试的题目数）
     * <p>
     * 通过 note='pk:{challengeId}' 关联到本对战，复用 portal_interview_submission。
     *
     * @param tag   对战打标，格式 "pk:{challengeId}"
     * @param userId 用户ID
     */
    @Select("SELECT COUNT(DISTINCT question_id) FROM portal_interview_submission " +
            "WHERE note = #{tag} AND user_id = #{userId}")
    int countAnsweredQuestions(@Param("tag") String tag, @Param("userId") Long userId);

    /**
     * 统计某用户在某场对战中的通过题数（is_success=1 的不同 question_id 数）
     */
    @Select("SELECT COUNT(DISTINCT question_id) FROM portal_interview_submission " +
            "WHERE note = #{tag} AND user_id = #{userId} AND is_success = 1")
    int countPassedQuestions(@Param("tag") String tag, @Param("userId") Long userId);

    /**
     * 公司题目挑战榜（3.7）：按 company_id 聚合用户通过题数
     * <p>
     * 复用 portal_interview_submission + portal_interview_question_company，未传 companyId 时聚合所有公司。
     * 返回每行 Map：{ user_id, nickname, avatar, company_id, company_name, passed_count }
     *
     * @param companyId 公司ID（可选）
     * @param limit     取前 N 名
     */
    @Select("<script>" +
            "SELECT s.user_id AS user_id, u.nickname AS nickname, u.avatar AS avatar, " +
            "c.company_id AS company_id, ic.name AS company_name, " +
            "COUNT(DISTINCT s.question_id) AS passed_count " +
            "FROM portal_interview_submission s " +
            "INNER JOIN portal_interview_question_company c ON c.question_id = s.question_id " +
            "LEFT JOIN portal_interview_company ic ON ic.id = c.company_id " +
            "LEFT JOIN portal_user u ON u.id = s.user_id " +
            "WHERE s.is_success = 1 " +
            "<if test='companyId != null'> AND c.company_id = #{companyId} </if>" +
            "GROUP BY s.user_id, u.nickname, u.avatar, c.company_id, ic.name " +
            "ORDER BY passed_count DESC, s.user_id ASC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> selectCompanyPkLeaderboard(@Param("companyId") Long companyId,
                                                          @Param("limit") int limit);
}
