package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
import com.moyun.ext.cms.service.IPortalInterviewPositionService;
import com.moyun.ext.cms.service.IUserProfileSnapshotService;
import com.moyun.portal.domain.entity.PortalInterviewPosition;
import com.moyun.portal.domain.entity.PortalMockInterview;
import com.moyun.portal.domain.entity.PortalUserStats;
import com.moyun.portal.mapper.PortalEntityTagMapper;
import com.moyun.portal.mapper.PortalMockInterviewMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户画像快照 Service 实现（v5.9 阶段0）
 * <p>
 * 数据来源：
 *  - portal_interview_position：岗位必备技能
 *  - portal_interview_submission（经 PortalEntityTagMapper.selectKnowledgeMastery）：薄弱点
 *  - portal_user_stats：面试统计
 *
 * @author moyun
 */
@Service
public class UserProfileSnapshotServiceImpl implements IUserProfileSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileSnapshotServiceImpl.class);

    /** 薄弱点判定阈值：失败率 ≥ 0.5 视为薄弱（即通过率 ≤ 50%） */
    private static final double WEAK_FAIL_RATE_THRESHOLD = 0.5;

    /** 薄弱点最少答题数：标签下用户至少答过 N 题才纳入薄弱点判定 */
    private static final int WEAK_MIN_TOTAL = 2;

    /** 薄弱点最多取 Top N */
    private static final int WEAK_TAG_LIMIT = 8;

    @Autowired private IPortalInterviewPositionService positionService;
    @Autowired private PortalEntityTagMapper entityTagMapper;
    @Autowired private PortalUserStatsMapper userStatsMapper;
    @Autowired private PortalMockInterviewMapper mockInterviewMapper;
    @Autowired private ObjectMapper objectMapper;

    // ========================================================================
    // 构建画像快照
    // ========================================================================
    @Override
    public UserProfileSnapshotVO buildSnapshot(Long userId, String position, String scene) {
        UserProfileSnapshotVO snapshot = new UserProfileSnapshotVO();
        snapshot.setUserId(userId);
        snapshot.setPosition(position);
        snapshot.setScene(scene);

        // 1. 岗位必备技能（命中岗位字典则填充）
        List<String> requiredSkills = resolveRequiredSkills(position);
        snapshot.setRequiredSkills(requiredSkills);

        // 2. 薄弱知识点（从答题历史计算）
        List<UserProfileSnapshotVO.WeakTagItem> weakTags = computeWeakTags(userId);
        snapshot.setWeakTags(weakTags);

        // 3. 面试统计
        PortalUserStats stats = userStatsMapper.selectByUserId(userId);
        if (stats != null) {
            snapshot.setMockInterviewCount(stats.getMockInterviewCount());
            snapshot.setAvgMockScore(stats.getAvgMockScore());
        }

        // 4. 是否命中画像驱动：薄弱点 ≥ 1 或必备技能 ≥ 1
        boolean personalized = (weakTags != null && !weakTags.isEmpty())
                || (requiredSkills != null && !requiredSkills.isEmpty());
        snapshot.setPersonalized(personalized);
        return snapshot;
    }

    // ========================================================================
    // 刷新薄弱知识点（写 portal_user_stats.weak_tags）
    // ========================================================================
    @Override
    public void refreshWeakTags(Long userId) {
        if (userId == null) return;
        try {
            List<UserProfileSnapshotVO.WeakTagItem> weakTags = computeWeakTags(userId);
            String json = toJson(weakTags);
            // 确保统计行存在
            userStatsMapper.insertIfNotExists(userId);
            userStatsMapper.updateWeakTags(userId, json);
        } catch (Exception e) {
            log.warn("[WeakTags] 刷新用户 {} 薄弱点失败：{}", userId, e.getMessage());
        }
    }

    // ========================================================================
    // 更新面试统计（次数 + 平均分）
    // ========================================================================
    @Override
    public void updateMockInterviewStats(Long userId) {
        if (userId == null) return;
        try {
            // 统计该用户所有已结束的面试
            LambdaQueryWrapper<PortalMockInterview> qw = new LambdaQueryWrapper<>();
            qw.eq(PortalMockInterview::getUserId, userId)
                    .eq(PortalMockInterview::getStatus, "finished");
            List<PortalMockInterview> list = mockInterviewMapper.selectList(qw);
            int count = list.size();
            int avg = 0;
            if (count > 0) {
                int sum = 0;
                for (PortalMockInterview m : list) {
                    if (m.getScore() != null) {
                        sum += m.getScore();
                    }
                }
                avg = (int) Math.round((double) sum / count);
            }
            userStatsMapper.insertIfNotExists(userId);
            userStatsMapper.updateMockInterviewStats(userId, count, avg);
        } catch (Exception e) {
            log.warn("[MockStats] 更新用户 {} 面试统计失败：{}", userId, e.getMessage());
        }
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /** 解析岗位必备技能（从岗位字典 required_skills JSON 数组） */
    private List<String> resolveRequiredSkills(String position) {
        if (StringUtils.isEmpty(position)) return new ArrayList<>();
        PortalInterviewPosition pos = positionService.findByName(position);
        if (pos == null || StringUtils.isEmpty(pos.getRequiredSkills())) {
            return new ArrayList<>();
        }
        List<String> skills = fromJsonList(pos.getRequiredSkills(), String.class);
        return skills == null ? new ArrayList<>() : skills;
    }

    /**
     * 计算用户薄弱知识点：
     *  - 复用 PortalEntityTagMapper.selectKnowledgeMastery 获取"标签→总数/通过数"
     *  - 失败率 = 1 - 通过率 = 1 - solved/total
     *  - 筛选失败率 ≥ 0.5 且总答题数 ≥ 2 的标签，按失败率降序取 Top 8
     */
    private List<UserProfileSnapshotVO.WeakTagItem> computeWeakTags(Long userId) {
        if (userId == null) return new ArrayList<>();
        List<Map<String, Object>> rows = entityTagMapper.selectKnowledgeMastery(userId);
        if (rows == null || rows.isEmpty()) return new ArrayList<>();

        List<UserProfileSnapshotVO.WeakTagItem> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long tagId = toLong(row.get("tag_id"));
            if (tagId == null) continue;
            int total = toInt(row.get("total"));
            int solved = toInt(row.get("solved"));
            if (total < WEAK_MIN_TOTAL) continue;
            double passRate = total > 0 ? (double) solved / total : 0;
            double failRate = 1.0 - passRate;
            if (failRate < WEAK_FAIL_RATE_THRESHOLD) continue;

            UserProfileSnapshotVO.WeakTagItem item = new UserProfileSnapshotVO.WeakTagItem();
            item.setTagId(tagId);
            item.setTotal(total);
            item.setSolved(solved);
            item.setFailRate(Math.round(failRate * 100) / 100.0);
            // 标签名从 tag_id 单独查太慢，这里通过 selectKnowledgeNodes 反查（仅一次）
            // 简化：在 buildSnapshot 时一次性查节点映射
            result.add(item);
        }
        // 补充 tagName
        Map<Long, String> nameMap = buildTagNameMap();
        for (UserProfileSnapshotVO.WeakTagItem item : result) {
            item.setTagName(nameMap.getOrDefault(item.getTagId(), "标签#" + item.getTagId()));
        }
        // 按失败率降序，取 Top N
        result.sort(Comparator.comparingDouble(
                (UserProfileSnapshotVO.WeakTagItem i) -> i.getFailRate() == null ? 0 : -i.getFailRate()));
        return result.size() > WEAK_TAG_LIMIT ? result.subList(0, WEAK_TAG_LIMIT) : result;
    }

    /** 一次性查询标签 ID→Name 映射（用于补充薄弱点的 tagName） */
    private Map<Long, String> buildTagNameMap() {
        List<Map<String, Object>> nodes = entityTagMapper.selectKnowledgeNodes(1000);
        if (nodes == null || nodes.isEmpty()) return Collections.emptyMap();
        return nodes.stream().collect(Collectors.toMap(
                r -> toLong(r.get("tag_id")),
                r -> toStr(r.get("tag_name")),
                (a, b) -> a
        ));
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("JSON List 反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static String toStr(Object o) {
        return o == null ? null : o.toString();
    }
}
