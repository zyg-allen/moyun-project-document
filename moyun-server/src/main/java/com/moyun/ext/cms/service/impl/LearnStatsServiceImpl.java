package com.moyun.ext.cms.service.impl;

import com.moyun.ext.cms.domain.vo.KnowledgeEdgeVO;
import com.moyun.ext.cms.domain.vo.KnowledgeGraphVO;
import com.moyun.ext.cms.domain.vo.KnowledgeNodeVO;
import com.moyun.ext.cms.domain.vo.LearnCalendarCellVO;
import com.moyun.ext.cms.domain.vo.LeaderboardItemVO;
import com.moyun.ext.cms.domain.vo.LeaderboardVO;
import com.moyun.ext.cms.service.ILearnStatsService;
import com.moyun.portal.mapper.PortalEntityTagMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习统计 Service 实现（阶段三 3.4 / 3.5 / 3.7）
 * <p>
 * 仅查询聚合，不新建任何表。
 *
 * @author moyun
 */
@Service
public class LearnStatsServiceImpl implements ILearnStatsService {

    /** 知识图谱节点上限 */
    private static final int KNOWLEDGE_NODE_LIMIT = 60;
    /** 知识图谱边上限 */
    private static final int KNOWLEDGE_EDGE_LIMIT = 50;
    /** 排行榜最大条目数 */
    private static final int LEADERBOARD_MAX_LIMIT = 100;

    @Autowired
    private PortalInterviewSubmissionMapper submissionMapper;

    @Autowired
    private PortalEntityTagMapper entityTagMapper;

    // ========================================================================
    // 3.4 刷题日历热力图
    // ========================================================================
    @Override
    public List<LearnCalendarCellVO> getCalendar(Long userId, Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        List<Map<String, Object>> rows = submissionMapper.selectCalendarByUserAndYear(userId, targetYear);
        List<LearnCalendarCellVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            LearnCalendarCellVO vo = new LearnCalendarCellVO();
            vo.setDate(toStr(row.get("date")));
            vo.setCount(toInt(row.get("count")));
            vo.setSuccessCount(toInt(row.get("success_count")));
            result.add(vo);
        }
        return result;
    }

    // ========================================================================
    // 3.5 知识图谱 / 标签云
    // ========================================================================
    @Override
    public KnowledgeGraphVO getKnowledgeGraph(Long userId) {
        // 1. 节点：标签 + 关联题目数
        List<Map<String, Object>> nodeRows = entityTagMapper.selectKnowledgeNodes(KNOWLEDGE_NODE_LIMIT);
        Map<Long, KnowledgeNodeVO> nodeMap = new HashMap<>(nodeRows.size());
        List<KnowledgeNodeVO> nodes = new ArrayList<>(nodeRows.size());
        for (Map<String, Object> row : nodeRows) {
            KnowledgeNodeVO node = new KnowledgeNodeVO();
            node.setTagId(toLong(row.get("tag_id")));
            node.setName(toStr(row.get("tag_name")));
            int questionCount = toInt(row.get("question_count"));
            node.setQuestionCount(questionCount);
            node.setTotal(questionCount);
            node.setSolved(0);
            node.setMastery(0);
            nodes.add(node);
            nodeMap.put(node.getTagId(), node);
        }

        // 2. 掌握度（仅当指定 userId 时计算）
        if (userId != null) {
            List<Map<String, Object>> masteryRows = entityTagMapper.selectKnowledgeMastery(userId);
            for (Map<String, Object> row : masteryRows) {
                Long tagId = toLong(row.get("tag_id"));
                KnowledgeNodeVO node = nodeMap.get(tagId);
                if (node == null) {
                    continue;
                }
                int total = toInt(row.get("total"));
                int solved = toInt(row.get("solved"));
                node.setTotal(total);
                node.setSolved(solved);
                node.setMastery(total > 0 ? Math.min(100, solved * 100 / total) : 0);
            }
        }

        // 3. 边：标签共现，仅保留两端节点都存在的边
        List<Map<String, Object>> edgeRows = entityTagMapper.selectKnowledgeEdges(KNOWLEDGE_EDGE_LIMIT);
        List<KnowledgeEdgeVO> edges = new ArrayList<>(edgeRows.size());
        for (Map<String, Object> row : edgeRows) {
            Long source = toLong(row.get("source"));
            Long target = toLong(row.get("target"));
            if (!nodeMap.containsKey(source) || !nodeMap.containsKey(target)) {
                continue;
            }
            KnowledgeEdgeVO edge = new KnowledgeEdgeVO();
            edge.setSource(source);
            edge.setTarget(target);
            edge.setWeight(toInt(row.get("weight")));
            edges.add(edge);
        }

        KnowledgeGraphVO vo = new KnowledgeGraphVO();
        vo.setNodes(nodes);
        vo.setEdges(edges);
        vo.setUserId(userId);
        return vo;
    }

    // ========================================================================
    // 3.7 排行榜 / PK
    // ========================================================================
    @Override
    public LeaderboardVO getLeaderboard(String type, Integer limit, Long currentUserId) {
        boolean isScore = "score".equalsIgnoreCase(type);
        String normalizedType = isScore ? "score" : "question";
        int top = clamp(limit == null ? LEADERBOARD_MAX_LIMIT : limit, 1, LEADERBOARD_MAX_LIMIT);

        List<Map<String, Object>> rows = isScore
                ? submissionMapper.selectScoreLeaderboard(top)
                : submissionMapper.selectQuestionCountLeaderboard(top);

        List<LeaderboardItemVO> list = new ArrayList<>(rows.size());
        int rank = 1;
        for (Map<String, Object> row : rows) {
            LeaderboardItemVO item = new LeaderboardItemVO();
            item.setRank(rank++);
            item.setUserId(toLong(row.get("user_id")));
            String nickname = toStr(row.get("nickname"));
            item.setNickname((nickname == null || nickname.isEmpty()) ? "匿名用户" : nickname);
            item.setAvatar(toStr(row.get("avatar")));
            int submitCount = toInt(row.get("submit_count"));
            int passedCount = toInt(row.get("passed_count"));
            int score = toInt(row.get("score"));
            item.setSubmitCount(submitCount);
            item.setPassedCount(passedCount);
            item.setScore(score);
            item.setValue(isScore ? score : passedCount);
            list.add(item);
        }

        LeaderboardVO vo = new LeaderboardVO();
        vo.setType(normalizedType);
        vo.setList(list);

        // 我的排名（未登录时跳过）
        if (currentUserId != null) {
            long myPassed = toLong(submissionMapper.selectPassedQuestionCount(currentUserId));
            long myScore = toLong(submissionMapper.selectLearnScore(currentUserId));
            long mySubmit = toLong(submissionMapper.selectSubmitCountByUser(currentUserId));
            vo.setMySubmitCount((int) mySubmit);
            vo.setMyPassedCount((int) myPassed);
            vo.setMyScore((int) myScore);
            vo.setMyValue(isScore ? (int) myScore : (int) myPassed);

            // 无任何提交记录时不返回名次，避免显示一个无意义的超大数字
            if (mySubmit > 0) {
                Long myRank = isScore
                        ? submissionMapper.selectLearnScoreRank(currentUserId)
                        : submissionMapper.selectQuestionCountRank(currentUserId);
                vo.setMyRank(myRank == null ? null : myRank.intValue());
            } else {
                vo.setMyRank(null);
            }
        }
        return vo;
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** 安全取 int，容忍 Long/BigInteger/BigDecimal/Null */
    private static int toInt(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 安全取 long，容忍 Long/BigInteger/BigDecimal/Null，null 返回 0L */
    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 安全取字符串，DATE/LocalDate 走 toString */
    private static String toStr(Object o) {
        if (o == null) {
            return null;
        }
        return o.toString();
    }
}
