package com.moyun.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.portal.domain.entity.PortalFreeTrial;
import com.moyun.portal.mapper.PortalFreeTrialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 免费体验次数服务（v11.85）
 *
 * <p>会员付费点统一体验机制：非会员每场景免费体验 {@code FREE_TRIAL_TIMES} 次，
 * 用完后引导开通会员（各 VIP Controller 的 status 接口返回 freeTrialLeft 供前端展示）。
 * 消耗原子化：先条件自增 UPDATE（防超扣），无行则 INSERT（唯一键冲突回退 UPDATE，防并发首插双跑）。
 *
 * @author moyun
 */
@Service
public class PortalFreeTrialService {

    /** 每场景免费体验次数（产品口径：2 次） */
    public static final int FREE_TRIAL_TIMES = 2;

    /** 场景：简历深度优化（AI 逐项建议/前后对比/采纳保存） */
    public static final String SCENE_RESUME_DEEP = "resume_deep";

    /** 场景：语音面试（按岗位/场景抽题开练） */
    public static final String SCENE_VOICE_INTERVIEW = "voice_interview";

    @Autowired
    private PortalFreeTrialMapper freeTrialMapper;

    /** 剩余免费体验次数（0~FREE_TRIAL_TIMES） */
    public int leftTimes(Long userId, String scene) {
        PortalFreeTrial record = freeTrialMapper.selectOne(new LambdaQueryWrapper<PortalFreeTrial>()
                .eq(PortalFreeTrial::getUserId, userId)
                .eq(PortalFreeTrial::getScene, scene)
                .last("LIMIT 1"));
        if (record == null || record.getUsedCount() == null) {
            return FREE_TRIAL_TIMES;
        }
        return Math.max(0, FREE_TRIAL_TIMES - record.getUsedCount());
    }

    /**
     * 原子消耗一次免费体验资格
     *
     * @return true=消耗成功（本次放行）；false=已用完（需会员）
     */
    public boolean tryConsume(Long userId, String scene) {
        int rows = freeTrialMapper.consumeExisting(userId, scene, FREE_TRIAL_TIMES);
        if (rows > 0) {
            return true;
        }
        try {
            return freeTrialMapper.insertFirst(userId, scene) > 0;
        } catch (DuplicateKeyException e) {
            // 并发首插撞唯一键：已有他人插入的行，回退条件自增（行存在但可能已满 → 0 即用完）
            return freeTrialMapper.consumeExisting(userId, scene, FREE_TRIAL_TIMES) > 0;
        }
    }
}
