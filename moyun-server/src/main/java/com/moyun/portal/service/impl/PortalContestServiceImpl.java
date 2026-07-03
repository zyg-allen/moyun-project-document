package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalContestSubmission;
import com.moyun.portal.domain.entity.PortalContestVote;
import com.moyun.portal.domain.entity.PortalWritingContest;
import com.moyun.portal.mapper.PortalContestSubmissionMapper;
import com.moyun.portal.mapper.PortalContestVoteMapper;
import com.moyun.portal.mapper.PortalWritingContestMapper;
import com.moyun.portal.service.IPortalContestService;
import com.moyun.util.bean.PageUtils;

/**
 * 创作挑战/征文活动 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalContestServiceImpl extends ServiceImpl<PortalWritingContestMapper, PortalWritingContest> implements IPortalContestService {

    @Autowired
    private PortalWritingContestMapper contestMapper;

    @Autowired
    private PortalContestSubmissionMapper submissionMapper;

    @Autowired
    private PortalContestVoteMapper voteMapper;

    @Override
    public Page<PortalWritingContest> listContests(PageDomain pageDomain, String status) {
        Page<PortalWritingContest> page = PageUtils.buildPage(pageDomain);
        LambdaQueryWrapper<PortalWritingContest> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PortalWritingContest::getStatus, status);
        } else {
            // 默认不展示 draft（草稿）
            wrapper.ne(PortalWritingContest::getStatus, "draft");
        }
        wrapper.orderByDesc(PortalWritingContest::getCreatedTime);
        return contestMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getContestDetail(Long contestId, Long currentUserId) {
        PortalWritingContest contest = contestMapper.selectById(contestId);
        Map<String, Object> result = new HashMap<>();
        if (contest == null) {
            return result;
        }
        result.put("contest", contest);

        // 投稿列表：按 vote_count 降序，再按 created_time 升序
        LambdaQueryWrapper<PortalContestSubmission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(PortalContestSubmission::getContestId, contestId)
                .ne(PortalContestSubmission::getStatus, "eliminated")
                .orderByDesc(PortalContestSubmission::getVoteCount)
                .orderByAsc(PortalContestSubmission::getCreatedTime);
        List<PortalContestSubmission> submissions = submissionMapper.selectList(subWrapper);
        result.put("submissions", submissions);

        // 当前用户已投票的投稿ID集合（用于前端展示 voted 标记）
        Set<Long> votedSubmissionIds = new HashSet<>();
        boolean hasSubmitted = false;
        if (currentUserId != null && !submissions.isEmpty()) {
            List<Long> submissionIds = submissions.stream()
                    .map(PortalContestSubmission::getId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<PortalContestVote> voteWrapper = new LambdaQueryWrapper<>();
            voteWrapper.eq(PortalContestVote::getUserId, currentUserId)
                    .in(PortalContestVote::getSubmissionId, submissionIds);
            List<PortalContestVote> votes = voteMapper.selectList(voteWrapper);
            votedSubmissionIds = votes.stream()
                    .map(PortalContestVote::getSubmissionId)
                    .collect(Collectors.toSet());

            // 是否已投稿
            hasSubmitted = submissions.stream()
                    .anyMatch(s -> currentUserId.equals(s.getUserId()));
        }
        result.put("votedSubmissionIds", votedSubmissionIds);
        result.put("hasSubmitted", hasSubmitted);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long contestId, Long userId, Long articleId) {
        // 校验活动存在
        PortalWritingContest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new RuntimeException("活动不存在");
        }
        // 简化：仅校验活动不是 draft 状态即可投稿
        if ("draft".equals(contest.getStatus())) {
            throw new RuntimeException("活动尚未开始");
        }
        // 校验同一用户对同一活动仅可投稿一次
        LambdaQueryWrapper<PortalContestSubmission> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(PortalContestSubmission::getContestId, contestId)
                .eq(PortalContestSubmission::getUserId, userId);
        Long existCount = submissionMapper.selectCount(dupWrapper);
        if (existCount != null && existCount > 0) {
            throw new RuntimeException("你已投稿过该活动，不可重复投稿");
        }
        // 校验同一活动同一文章不可重复投稿
        LambdaQueryWrapper<PortalContestSubmission> dupArticleWrapper = new LambdaQueryWrapper<>();
        dupArticleWrapper.eq(PortalContestSubmission::getContestId, contestId)
                .eq(PortalContestSubmission::getArticleId, articleId);
        Long existArticleCount = submissionMapper.selectCount(dupArticleWrapper);
        if (existArticleCount != null && existArticleCount > 0) {
            throw new RuntimeException("该文章已被投稿到本次活动");
        }

        PortalContestSubmission submission = new PortalContestSubmission();
        submission.setContestId(contestId);
        submission.setUserId(userId);
        submission.setArticleId(articleId);
        submission.setStatus("pending");
        submission.setVoteCount(0);
        submission.setCreatedTime(LocalDateTime.now());
        submissionMapper.insert(submission);
        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleVote(Long submissionId, Long userId) {
        PortalContestSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("投稿不存在");
        }

        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<PortalContestVote> voteWrapper = new LambdaQueryWrapper<>();
        voteWrapper.eq(PortalContestVote::getSubmissionId, submissionId)
                .eq(PortalContestVote::getUserId, userId);
        PortalContestVote existing = voteMapper.selectOne(voteWrapper);

        boolean voted;
        int delta;
        if (existing == null) {
            // 投票
            PortalContestVote vote = new PortalContestVote();
            vote.setSubmissionId(submissionId);
            vote.setUserId(userId);
            vote.setContestId(submission.getContestId());
            vote.setCreatedTime(LocalDateTime.now());
            voteMapper.insert(vote);
            voted = true;
            delta = 1;
        } else {
            // 取消投票
            voteMapper.deleteById(existing.getId());
            voted = false;
            delta = -1;
        }

        // 原子更新投票数（递减时使用 GREATEST 防止负数）
        submissionMapper.update(null,
                new LambdaUpdateWrapper<PortalContestSubmission>()
                        .eq(PortalContestSubmission::getId, submissionId)
                        .setSql("vote_count = GREATEST(vote_count + " + delta + ", 0)"));

        // 查询最新投票数
        PortalContestSubmission latest = submissionMapper.selectById(submissionId);
        result.put("voted", voted);
        result.put("voteCount", latest != null && latest.getVoteCount() != null ? latest.getVoteCount() : 0);
        return result;
    }

    @Override
    public Page<PortalContestSubmission> listMySubmissions(Long userId, PageDomain pageDomain) {
        Page<PortalContestSubmission> page = PageUtils.buildPage(pageDomain);
        LambdaQueryWrapper<PortalContestSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalContestSubmission::getUserId, userId)
                .orderByDesc(PortalContestSubmission::getCreatedTime);
        return submissionMapper.selectPage(page, wrapper);
    }
}
