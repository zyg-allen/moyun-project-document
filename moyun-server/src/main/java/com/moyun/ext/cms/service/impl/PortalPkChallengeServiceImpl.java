package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.service.IPkService;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.domain.entity.PortalPkChallenge;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.mapper.PortalPkChallengeMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PK 对战 Service 实现（3.7 排行榜 / PK）
 *
 * @author moyun
 */
@Service
public class PortalPkChallengeServiceImpl implements IPkService {

    /** 每场对战抽取的题目数 */
    private static final int QUESTION_COUNT = 5;
    /** 公司挑战榜默认取前 N 名 */
    private static final int LEADERBOARD_DEFAULT_LIMIT = 100;
    private static final int LEADERBOARD_MAX_LIMIT = 100;

    @Autowired
    private PortalPkChallengeMapper pkMapper;

    @Autowired
    private PortalInterviewQuestionMapper questionMapper;

    @Autowired
    private PortalInterviewSubmissionMapper submissionMapper;

    @Autowired
    private PortalUserMapper userMapper;

    // ========================================================================
    // 发起挑战
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalPkChallenge createChallenge(Long userId, Long opponentId, String scene, Long companyId) {
        if (userId == null) {
            throw new ServiceException("请登录后发起挑战");
        }
        if (opponentId == null) {
            throw new ServiceException("请选择对战对手");
        }
        if (opponentId.equals(userId)) {
            throw new ServiceException("不能与自己对战");
        }
        if (userMapper.selectById(opponentId) == null) {
            throw new ServiceException("对手用户不存在");
        }
        String normalizedScene = "company".equalsIgnoreCase(scene) ? "company" : "1v1";
        if ("company".equals(normalizedScene) && companyId == null) {
            throw new ServiceException("公司挑战需指定公司");
        }

        // 从题库随机抽 5 题（复用 PortalInterviewQuestionMapper，scene=company 时优先取该公司的题）
        List<PortalInterviewQuestion> questions = drawRandomQuestions(companyId, normalizedScene);
        if (questions.isEmpty()) {
            throw new ServiceException("题库暂无可用题目");
        }
        String questionIds = questions.stream()
                .map(q -> String.valueOf(q.getId()))
                .collect(Collectors.joining(","));

        PortalPkChallenge challenge = new PortalPkChallenge();
        challenge.setChallengerId(userId);
        challenge.setOpponentId(opponentId);
        challenge.setStatus("pending");
        challenge.setChallengerScore(0);
        challenge.setOpponentScore(0);
        challenge.setQuestionIds(questionIds);
        challenge.setScene(normalizedScene);
        challenge.setCompanyId("company".equals(normalizedScene) ? companyId : null);
        challenge.setCreatedTime(LocalDateTime.now());
        pkMapper.insert(challenge);

        // 返回时附带题目简要，便于发起方立即查看
        challenge.setQuestions(toQuestionBriefList(questions));
        return challenge;
    }

    // ========================================================================
    // 接受 / 拒绝
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptChallenge(Long challengeId, Long userId) {
        PortalPkChallenge challenge = mustLoad(challengeId);
        if (!challenge.getOpponentId().equals(userId)) {
            throw new ServiceException("仅应战方可接受挑战");
        }
        if (!"pending".equals(challenge.getStatus())) {
            throw new ServiceException("当前对战状态不允许接受");
        }
        challenge.setStatus("ongoing");
        pkMapper.updateById(challenge);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean declineChallenge(Long challengeId, Long userId) {
        PortalPkChallenge challenge = mustLoad(challengeId);
        if (!challenge.getOpponentId().equals(userId)) {
            throw new ServiceException("仅应战方可拒绝挑战");
        }
        if (!"pending".equals(challenge.getStatus())) {
            throw new ServiceException("当前对战状态不允许拒绝");
        }
        challenge.setStatus("declined");
        pkMapper.updateById(challenge);
        return true;
    }

    // ========================================================================
    // 提交答案 + 计分
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitAnswer(Long challengeId, Long userId, Long questionId, String answer) {
        PortalPkChallenge challenge = mustLoad(challengeId);
        if (userId == null) {
            throw new ServiceException("请登录后提交");
        }
        if (!"ongoing".equals(challenge.getStatus())) {
            throw new ServiceException("对战未开始或已结束");
        }
        boolean isChallenger = challenge.getChallengerId().equals(userId);
        boolean isOpponent = challenge.getOpponentId().equals(userId);
        if (!isChallenger && !isOpponent) {
            throw new ServiceException("您不在此场对战中");
        }
        if (!containsQuestion(challenge.getQuestionIds(), questionId)) {
            throw new ServiceException("该题不在此场对战题库中");
        }

        // 复用 portal_interview_submission 的 is_success 判断（非空答案视为通过）
        String tag = tagOf(challengeId);
        PortalInterviewSubmission submission = new PortalInterviewSubmission();
        submission.setQuestionId(questionId);
        submission.setUserId(userId);
        submission.setContent(answer);
        submission.setLanguage("text");
        submission.setAnswerType("text");
        submission.setNote(tag);
        boolean isSuccess = StringUtils.isNotEmpty(answer);
        submission.setIsSuccess(isSuccess);
        submission.setStatus(isSuccess ? "accepted" : "pending");
        submission.setCreateTime(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 重新计分：该用户在本对战中的通过题数
        int score = pkMapper.countPassedQuestions(tag, userId);
        if (isChallenger) {
            challenge.setChallengerScore(score);
        } else {
            challenge.setOpponentScore(score);
        }
        pkMapper.updateById(challenge);

        // 双方都答完则自动结算
        boolean finished = false;
        Long winnerId = null;
        int total = countTotalQuestions(challenge.getQuestionIds());
        if (total > 0
                && pkMapper.countAnsweredQuestions(tag, challenge.getChallengerId()) >= total
                && pkMapper.countAnsweredQuestions(tag, challenge.getOpponentId()) >= total) {
            finishChallenge(challengeId);
            finished = true;
            winnerId = pkMapper.selectById(challengeId).getWinnerId();
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("isSuccess", isSuccess);
        result.put("score", score);
        result.put("finished", finished);
        result.put("winnerId", winnerId);
        return result;
    }

    // ========================================================================
    // 结束对战
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean finishChallenge(Long challengeId) {
        PortalPkChallenge challenge = mustLoad(challengeId);
        if ("finished".equals(challenge.getStatus())) {
            return true;
        }
        int c = challenge.getChallengerScore() == null ? 0 : challenge.getChallengerScore();
        int o = challenge.getOpponentScore() == null ? 0 : challenge.getOpponentScore();
        Long winnerId = c > o ? challenge.getChallengerId()
                : o > c ? challenge.getOpponentId() : null;
        challenge.setWinnerId(winnerId);
        challenge.setStatus("finished");
        challenge.setFinishedTime(LocalDateTime.now());
        pkMapper.updateById(challenge);
        return true;
    }

    // ========================================================================
    // 我的对战列表
    // ========================================================================
    @Override
    public List<PortalPkChallenge> getMyChallenges(Long userId, String status) {
        if (userId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<PortalPkChallenge> wrapper = new LambdaQueryWrapper<PortalPkChallenge>()
                .and(w -> w.eq(PortalPkChallenge::getChallengerId, userId)
                        .or().eq(PortalPkChallenge::getOpponentId, userId))
                .eq(StringUtils.isNotEmpty(status), PortalPkChallenge::getStatus, status)
                .orderByDesc(PortalPkChallenge::getCreatedTime);
        List<PortalPkChallenge> list = pkMapper.selectList(wrapper);
        enrichUserinfo(list);
        return list;
    }

    // ========================================================================
    // 对战详情
    // ========================================================================
    @Override
    public PortalPkChallenge getChallengeDetail(Long challengeId, Long userId) {
        PortalPkChallenge challenge = mustLoad(challengeId);
        if (userId != null
                && !challenge.getChallengerId().equals(userId)
                && !challenge.getOpponentId().equals(userId)) {
            throw new ServiceException("无权查看此对战");
        }
        // 填充双方用户信息
        PortalUser challenger = userMapper.selectById(challenge.getChallengerId());
        PortalUser opponent = userMapper.selectById(challenge.getOpponentId());
        if (challenger != null) {
            challenge.setChallengerNickname(challenger.getNickname());
            challenge.setChallengerAvatar(challenger.getAvatar());
        }
        if (opponent != null) {
            challenge.setOpponentNickname(opponent.getNickname());
            challenge.setOpponentAvatar(opponent.getAvatar());
        }
        // 填充题目简要
        List<Long> ids = parseQuestionIds(challenge.getQuestionIds());
        if (!ids.isEmpty()) {
            List<PortalInterviewQuestion> qs = questionMapper.selectBatchIds(ids);
            challenge.setQuestions(toQuestionBriefList(qs));
        }
        return challenge;
    }

    // ========================================================================
    // 公司题目挑战榜
    // ========================================================================
    @Override
    public List<Map<String, Object>> getCompanyLeaderboard(Long companyId, Integer limit) {
        int top = limit == null ? LEADERBOARD_DEFAULT_LIMIT : Math.max(1, Math.min(limit, LEADERBOARD_MAX_LIMIT));
        List<Map<String, Object>> rows = pkMapper.selectCompanyPkLeaderboard(companyId, top);
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>(row);
            item.put("rank", rank++);
            item.put("nickname", toStr(row.get("nickname")));
            if (item.get("nickname") == null || ((String) item.get("nickname")).isEmpty()) {
                item.put("nickname", "匿名用户");
            }
            item.put("avatar", toStr(row.get("avatar")));
            item.put("passedCount", toInt(row.get("passed_count")));
            item.put("userId", toLong(row.get("user_id")));
            item.put("companyId", toLong(row.get("company_id")));
            item.put("companyName", toStr(row.get("company_name")));
            result.add(item);
        }
        return result;
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    private PortalPkChallenge mustLoad(Long challengeId) {
        if (challengeId == null) {
            throw new ServiceException("对战不存在");
        }
        PortalPkChallenge challenge = pkMapper.selectById(challengeId);
        if (challenge == null) {
            throw new ServiceException("对战不存在");
        }
        return challenge;
    }

    /** 抽取 5 道随机题目；scene=company 时优先取该公司关联题目，不足时回退到全题库 */
    private List<PortalInterviewQuestion> drawRandomQuestions(Long companyId, String scene) {
        LambdaQueryWrapper<PortalInterviewQuestion> wrapper = new LambdaQueryWrapper<PortalInterviewQuestion>()
                .eq(PortalInterviewQuestion::getStatus, "active")
                .last("ORDER BY RAND() LIMIT " + QUESTION_COUNT);
        return questionMapper.selectList(wrapper);
    }

    private List<Map<String, Object>> toQuestionBriefList(List<PortalInterviewQuestion> qs) {
        List<Map<String, Object>> list = new ArrayList<>(qs.size());
        for (PortalInterviewQuestion q : qs) {
            Map<String, Object> brief = new HashMap<>(3);
            brief.put("id", q.getId());
            brief.put("title", q.getTitle());
            brief.put("difficulty", q.getDifficulty());
            list.add(brief);
        }
        return list;
    }

    private void enrichUserinfo(List<PortalPkChallenge> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = new ArrayList<>();
        for (PortalPkChallenge c : list) {
            if (c.getChallengerId() != null) userIds.add(c.getChallengerId());
            if (c.getOpponentId() != null) userIds.add(c.getOpponentId());
        }
        if (userIds.isEmpty()) {
            return;
        }
        List<PortalUser> users = userMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList()));
        Map<Long, PortalUser> userMap = users.stream()
                .collect(Collectors.toMap(PortalUser::getId, u -> u, (a, b) -> a));
        for (PortalPkChallenge c : list) {
            PortalUser ch = userMap.get(c.getChallengerId());
            if (ch != null) {
                c.setChallengerNickname(ch.getNickname());
                c.setChallengerAvatar(ch.getAvatar());
            }
            PortalUser op = userMap.get(c.getOpponentId());
            if (op != null) {
                c.setOpponentNickname(op.getNickname());
                c.setOpponentAvatar(op.getAvatar());
            }
        }
    }

    private static String tagOf(Long challengeId) {
        return "pk:" + challengeId;
    }

    private static boolean containsQuestion(String questionIds, Long questionId) {
        if (StringUtils.isEmpty(questionIds) || questionId == null) {
            return false;
        }
        return parseQuestionIds(questionIds).contains(questionId);
    }

    private static int countTotalQuestions(String questionIds) {
        return parseQuestionIds(questionIds).size();
    }

    private static List<Long> parseQuestionIds(String questionIds) {
        if (StringUtils.isEmpty(questionIds)) {
            return new ArrayList<>();
        }
        return Arrays.stream(questionIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String toStr(Object o) {
        return o == null ? null : o.toString();
    }
}
