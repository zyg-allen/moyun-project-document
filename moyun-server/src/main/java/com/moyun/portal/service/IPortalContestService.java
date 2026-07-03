package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalContestSubmission;
import com.moyun.portal.domain.entity.PortalWritingContest;

import java.util.Map;

/**
 * 创作挑战/征文活动 业务层
 *
 * 简化实现：仅做基础投稿+投票+展示，不接入 Flowable 评审流程。
 *
 * @author moyun
 */
public interface IPortalContestService extends IService<PortalWritingContest> {

    /**
     * 公开活动列表（仅 collecting/voting/ended 状态，按 created_time 降序）
     */
    Page<PortalWritingContest> listContests(PageDomain pageDomain, String status);

    /**
     * 公开活动详情（含投稿列表，已登录则附带当前用户对每条投稿的投票标记）
     */
    Map<String, Object> getContestDetail(Long contestId, Long currentUserId);

    /**
     * 投稿（需登录）：同一活动同一用户仅可投稿一次
     *
     * @return 投稿ID
     */
    Long submit(Long contestId, Long userId, Long articleId);

    /**
     * 投票（需登录，toggle）：同一用户对同一投稿仅可投一票，再次投票取消
     *
     * @return 操作后的投票状态与票数
     */
    Map<String, Object> toggleVote(Long submissionId, Long userId);

    /**
     * 我的投稿（需登录）
     */
    Page<PortalContestSubmission> listMySubmissions(Long userId, PageDomain pageDomain);
}
