package com.moyun.vip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.vip.domain.entity.VipUserCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户会员卡 Mapper
 *
 * @author moyun
 */
@Mapper
public interface VipUserCardMapper extends BaseMapper<VipUserCard> {

    /**
     * 单条原子续期（付款发卡 / 续费顺延）
     *
     * <p><b>为什么必须是"单条 UPDATE"而不是"读出来改再写回"</b>：
     * 续费是 read-modify-write，两个**不同订单**的并发回调会各自读到同一个
     * {@code expire_time}，各自 +N 天写回 → <b>丢一次续费</b>（用户付两次只得一期）。
     * {@code pay_order} 的状态机只能挡住**同一订单**的重复回调，挡不住两笔订单并发。
     * 而把锁加在 {@code VipServiceImpl.grantCard} 里也没用——该方法位于支付回调**事务内部**，
     * try-with-resources 会在事务提交**之前**释放锁，第二个事务仍会读到旧快照。
     * 因此改为把"读-算-写"压进一条 SQL：InnoDB 行锁天然串行化，无需分布式锁、无需改网关。</p>
     *
     * <p>续期语义（与历史 {@code VipServiceImpl.grantCard} 逐条对齐，不是重新设计）：</p>
     * <ul>
     *   <li>起点 = {@code GREATEST(COALESCE(expire_time, NOW()), NOW())}
     *       —— 未过期从原到期顺延（不吃掉剩余天数），已过期/永久（NULL）从 NOW() 重算；</li>
     *   <li>{@code duration_days = -1} → {@code expire_time = NULL}（永久）；</li>
     *   <li>{@code status} 置 1（被后台作废的卡在重新购买后恢复有效）；</li>
     *   <li>不区分 {@code status} / 是否过期：一个 {@code (user_id, platform_code)} 恒只有一行
     *       （与 DDL 的 {@code uk_user_platform} 与表注释"一端一卡，续费顺延"一致）。</li>
     * </ul>
     *
     * @param userId       用户 ID
     * @param platformCode 端代码
     * @param tierCode     本次购买/续费的等级代码（覆盖式，与历史行为一致）
     * @param orderId      本次支付单 ID（记录最近一次）
     * @param durationDays 有效天数（-1=永久，0=免费，正数=天数）；调用方保证非 null
     * @return 受影响行数；<b>=0 表示该端无卡</b>（需插入首卡），&gt;0 表示续期完成
     */
    int renewCard(@Param("userId") Long userId,
                  @Param("platformCode") String platformCode,
                  @Param("tierCode") String tierCode,
                  @Param("orderId") Long orderId,
                  @Param("durationDays") Integer durationDays);
}
