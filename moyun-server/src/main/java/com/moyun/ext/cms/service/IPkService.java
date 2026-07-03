package com.moyun.ext.cms.service;

import com.moyun.portal.domain.entity.PortalPkChallenge;

import java.util.List;
import java.util.Map;

/**
 * PK 对战 Service（3.7 排行榜 / PK）
 * <p>
 * 采用异步对战模式：发起方抽题生成对战，应战方接受后双方各自答题，全员答完自动结算。
 * 题目复用 portal_interview_question，答题提交复用 portal_interview_submission（通过 note 打标关联）。
 *
 * @author moyun
 */
public interface IPkService {

    /**
     * 发起挑战：从题库随机抽 5 题生成对战
     *
     * @param userId     发起方用户ID
     * @param opponentId 应战方用户ID
     * @param scene      场景：1v1 / company
     * @param companyId  公司ID（scene=company 时必填，否则可空）
     */
    PortalPkChallenge createChallenge(Long userId, Long opponentId, String scene, Long companyId);

    /**
     * 接受挑战：状态 pending -> ongoing
     */
    boolean acceptChallenge(Long challengeId, Long userId);

    /**
     * 拒绝挑战：状态 pending -> declined
     */
    boolean declineChallenge(Long challengeId, Long userId);

    /**
     * 提交某题答案并计分；双方全部答完时自动结算。
     *
     * @return { isSuccess, score, finished, winnerId }
     */
    Map<String, Object> submitAnswer(Long challengeId, Long userId, Long questionId, String answer);

    /**
     * 结束对战，计算 winner
     */
    boolean finishChallenge(Long challengeId);

    /**
     * 我的对战列表（作为发起方或应战方），可按 status 筛选
     */
    List<PortalPkChallenge> getMyChallenges(Long userId, String status);

    /**
     * 对战详情（含双方昵称/头像与题目简要）
     */
    PortalPkChallenge getChallengeDetail(Long challengeId, Long userId);

    /**
     * 公司题目挑战榜：按 company_id 聚合用户通过题数
     *
     * @param companyId 公司ID（可选，为空时聚合所有公司）
     * @param limit    取前 N 名
     */
    List<Map<String, Object>> getCompanyLeaderboard(Long companyId, Integer limit);
}
