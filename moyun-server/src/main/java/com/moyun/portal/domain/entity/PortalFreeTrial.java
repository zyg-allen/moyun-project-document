package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 免费体验次数记录（v11.85）
 *
 * <p>会员付费点统一体验机制：非会员每场景可免费体验 {@code FREE_TRIAL_TIMES} 次（当前 2 次），
 * 用完后引导开通会员。按 user_id + scene 唯一记录，原子消耗（UPDATE 条件自增 / INSERT 冲突即失败）。
 *
 * <p>场景（scene）：
 * <ul>
 *     <li>resume_deep：简历深度优化（AI 逐项建议/前后对比/采纳保存）</li>
 *     <li>voice_interview：语音面试（按岗位/场景抽题开练）</li>
 * </ul>
 *
 * @author moyun
 */
@Data
@TableName("portal_free_trial")
public class PortalFreeTrial {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID（portal_user.id） */
    private Long userId;

    /** 场景编码（resume_deep / voice_interview） */
    private String scene;

    /** 已使用次数 */
    private Integer usedCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
