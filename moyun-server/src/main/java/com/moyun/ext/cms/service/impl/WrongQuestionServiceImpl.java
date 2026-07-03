package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.query.WrongQuestionQuery;
import com.moyun.ext.cms.domain.vo.WrongQuestionVO;
import com.moyun.ext.cms.service.IWrongQuestionService;
import com.moyun.portal.domain.entity.PortalWrongQuestion;
import com.moyun.portal.mapper.PortalWrongQuestionMapper;
import com.moyun.util.bean.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 错题本 Service 实现（任务 3.3）
 *
 * @author moyun
 */
@Service
public class WrongQuestionServiceImpl implements IWrongQuestionService {

    /** 艾宾浩斯复习间隔（分钟）：1天 → 2天 → 4天 → 7天 → 15天 */
    private static final int[] REVIEW_INTERVALS_MINUTES = {
            24 * 60, 2 * 24 * 60, 4 * 24 * 60, 7 * 24 * 60, 15 * 24 * 60
    };

    @Autowired private PortalWrongQuestionMapper wrongQuestionMapper;

    // ========================================================================
    // 错题列表（分页）
    // ========================================================================
    @Override
    public Page<WrongQuestionVO> listWrongQuestions(Long userId, WrongQuestionQuery query) {
        Page<WrongQuestionVO> page = PageUtils.buildPage(query);
        return wrongQuestionMapper.selectWrongQuestionPage(page, userId, query);
    }

    // ========================================================================
    // 标记已掌握
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markMastered(Long userId, Long questionId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        int rows = wrongQuestionMapper.markMastered(userId, questionId);
        if (rows == 0) {
            throw new ServiceException("错题不存在或已掌握");
        }
        return rows;
    }

    // ========================================================================
    // 今日待复习
    // ========================================================================
    @Override
    public List<WrongQuestionVO> listTodayReview(Long userId) {
        WrongQuestionQuery query = new WrongQuestionQuery();
        query.setStatus("reviewing");
        // 今日待复习按 next_review_time <= now 过滤，复用列表查询后内存过滤
        Page<WrongQuestionVO> page = PageUtils.buildPage(1, 100);
        Page<WrongQuestionVO> result = wrongQuestionMapper.selectWrongQuestionPage(page, userId, query);
        List<WrongQuestionVO> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (result.getRecords() != null) {
            for (WrongQuestionVO vo : result.getRecords()) {
                if (vo.getNextReviewTime() != null && !vo.getNextReviewTime().isAfter(now)) {
                    list.add(vo);
                }
            }
        }
        return list;
    }

    // ========================================================================
    // 答题失败时自动加入错题本（幂等：已存在则递增 wrong_count）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordWrongQuestion(Long userId, Long questionId, Long attemptId) {
        if (userId == null || questionId == null) {
            return null;
        }
        PortalWrongQuestion existing = wrongQuestionMapper.selectByUserAndQuestion(userId, questionId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            PortalWrongQuestion wq = new PortalWrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setAttemptId(attemptId);
            wq.setStatus("wrong");
            wq.setWrongCount(1);
            wq.setLastWrongTime(now);
            wq.setNextReviewTime(now.plusMinutes(REVIEW_INTERVALS_MINUTES[0]));
            wq.setCreatedTime(now);
            try {
                wrongQuestionMapper.insert(wq);
                return wq.getId();
            } catch (DuplicateKeyException e) {
                // 并发兜底：uk_user_question 触发，回退到更新已存在的记录
                existing = wrongQuestionMapper.selectByUserAndQuestion(userId, questionId);
                if (existing == null) {
                    throw e;
                }
                bumpWrongCount(existing, attemptId, now);
                return existing.getId();
            }
        }
        // 已存在 → 递增 wrong_count，状态回退到 wrong（如果已是 mastered/reviewing）
        bumpWrongCount(existing, attemptId, now);
        return existing.getId();
    }

    private void bumpWrongCount(PortalWrongQuestion existing, Long attemptId, LocalDateTime now) {
        existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
        existing.setAttemptId(attemptId);
        existing.setLastWrongTime(now);
        existing.setNextReviewTime(now.plusMinutes(nextReviewInterval(existing.getWrongCount())));
        if (!"wrong".equals(existing.getStatus())) {
            existing.setStatus("wrong");
        }
        wrongQuestionMapper.updateById(existing);
    }

    /**
     * 根据已错次数选择下一个复习间隔（超过数组长度则取最后一个）
     */
    private int nextReviewInterval(int wrongCount) {
        int idx = Math.min(wrongCount - 1, REVIEW_INTERVALS_MINUTES.length - 1);
        if (idx < 0) {
            idx = 0;
        }
        return REVIEW_INTERVALS_MINUTES[idx];
    }

    // ========================================================================
    // 统计 / 最近错题
    // ========================================================================
    @Override
    public Long countWrong(Long userId, String status) {
        return wrongQuestionMapper.countByUserAndStatus(userId, status);
    }

    @Override
    public Long countTodayReview(Long userId) {
        return wrongQuestionMapper.countTodayReview(userId);
    }

    @Override
    public List<WrongQuestionVO> listRecentWrong(Long userId, int limit) {
        List<PortalWrongQuestion> entities = wrongQuestionMapper.selectRecentWrong(userId, limit);
        List<WrongQuestionVO> list = new ArrayList<>(entities == null ? 0 : entities.size());
        if (entities != null) {
            for (PortalWrongQuestion wq : entities) {
                WrongQuestionVO vo = new WrongQuestionVO();
                vo.setId(wq.getId());
                vo.setUserId(wq.getUserId());
                vo.setQuestionId(wq.getQuestionId());
                vo.setAttemptId(wq.getAttemptId());
                vo.setStatus(wq.getStatus());
                vo.setWrongCount(wq.getWrongCount());
                vo.setLastWrongTime(wq.getLastWrongTime());
                vo.setNextReviewTime(wq.getNextReviewTime());
                vo.setCreatedTime(wq.getCreatedTime());
                list.add(vo);
            }
        }
        return list;
    }
}
