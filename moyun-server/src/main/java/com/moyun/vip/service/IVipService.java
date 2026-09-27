package com.moyun.vip.service;

import java.util.List;
import java.util.Map;

/**
 * 统一 VIP 服务（与权限体系 @ss.hasPermi 同构）
 *
 * @author moyun
 */
public interface IVipService {

    /**
     * 端级 VIP 开关（sys_config.vip.enabled，端级优先全局兜底，30 分钟缓存）
     */
    boolean isVipEnabled(String platformCode);

    /** 清空开关缓存（后台改配置后调用） */
    void clearEnabledCache();

    /**
     * 用户在某端是否有效会员（存在有效会员卡）
     */
    boolean isVip(Long userId, String platformCode);

    /**
     * 用户在某端是否享有指定权益（仅校验不消耗）
     *
     * <p>判定链：会员卡定级（无卡=free）→ 等级权益额度 → 周期内已用次数 &lt; 额度
     */
    boolean hasBenefit(Long userId, String platformCode, String benefitCode);

    /**
     * 消耗一次权益（校验通过后原子计数：Redis incr + 异步落库）
     *
     * @return true=消耗成功；false=额度不足或无权益
     */
    boolean consumeBenefit(Long userId, String platformCode, String benefitCode);

    /**
     * 用户当前等级代码（无卡/过期=free）
     */
    String currentTierCode(Long userId, String platformCode);

    /**
     * 端级售卖视图：等级列表 + 各等级权益清单（门户/记账会员页共用）
     */
    List<Map<String, Object>> listTiers(String platformCode);

    /**
     * 用户会员详情：会员卡 + 权益使用情况（已用/剩余）
     */
    Map<String, Object> getVipDetail(Long userId, String platformCode);

    /**
     * 支付成功发卡/续费顺延（VipPayCallbackHandler 调用）
     *
     * <p><b>契约要点</b>（调用方与后续维护者必读）：</p>
     * <ul>
     *   <li>幂等于 {@code (userId, platformCode)}：<b>一个端恒只有一行会员卡</b>
     *       （DDL 侧由 {@code uk_user_platform} 保证）。续费/升级为**就地续期**，
     *       不会新增行；过期卡续费同样就地续期，起点取 {@code GREATEST(原到期, NOW())}。</li>
     *   <li>并发安全：续期是**单条原子 UPDATE**（{@code VipUserCardMapper.renewCard}），
     *       两笔订单并发不会丢更新；首购竞态由唯一键兜底后转为续期。</li>
     *   <li>本方法在**支付回调事务内**执行。抛异常 = 支付单回滚、渠道重试；
     *       因此"配置类错误"必须给可操作文案（如等级缺有效天数），而不是让它变成一行 NPE。</li>
     *   <li>副作用（有意）：后台"作废会员卡"（{@code status=0}）后再次购买，会在同一行恢复有效。</li>
     * </ul>
     *
     * @param orderId pay_order.id
     */
    void grantCard(Long userId, String platformCode, String tierCode, Long orderId);
}
