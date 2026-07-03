package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.KnowledgeGraphVO;
import com.moyun.ext.cms.domain.vo.LearnCalendarCellVO;
import com.moyun.ext.cms.domain.vo.LeaderboardVO;

import java.util.List;

/**
 * 学习统计 Service 接口（阶段三 3.4 / 3.5 / 3.7）
 * <p>
 * 仅做查询聚合，不新建任何表：
 * <ul>
 *   <li>3.4 刷题日历：聚合 portal_interview_submission.user_id + create_time</li>
 *   <li>3.5 知识图谱：聚合 portal_entity_tag（entity_type='interview_question'）</li>
 *   <li>3.7 排行榜：聚合 portal_interview_submission 通过题目数 / 刷题积分</li>
 * </ul>
 *
 * @author moyun
 */
public interface ILearnStatsService {

    /**
     * 刷题日历热力图（3.4，需登录）
     *
     * @param userId 门户用户ID（必传）
     * @param year   年份，null 时取当前年份
     */
    List<LearnCalendarCellVO> getCalendar(Long userId, Integer year);

    /**
     * 知识图谱 / 标签云（3.5，公开）
     *
     * @param userId 门户用户ID（可选，传入时计算该用户的掌握度）
     */
    KnowledgeGraphVO getKnowledgeGraph(Long userId);

    /**
     * 排行榜（3.7，公开）
     *
     * @param type         question=通过题目数榜 / score=刷题积分榜
     * @param limit        取前 N 名，上限 100
     * @param currentUserId 当前登录用户ID（可选，用于返回"我的排名"）
     */
    LeaderboardVO getLeaderboard(String type, Integer limit, Long currentUserId);
}
