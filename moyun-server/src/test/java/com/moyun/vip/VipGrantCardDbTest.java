package com.moyun.vip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.vip.domain.entity.VipTier;
import com.moyun.vip.domain.entity.VipUserCard;
import com.moyun.vip.mapper.VipTierMapper;
import com.moyun.vip.mapper.VipUserCardMapper;
import com.moyun.vip.service.IVipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VIP 发卡 —— 真实 MySQL 上的续期语义验证（{@code @SpringBootTest} + {@code @Transactional} 回滚）
 *
 * <p><b>为什么必须有一条真库测试</b>：`renewCard` 把"读-算-写"压进了一条 SQL，
 * 其中的坑**静态看代码看不出来、Mock 也测不出来**，只有真库能证伪：</p>
 * <ol>
 *   <li><b>SET 赋值顺序</b>：MySQL 的 {@code UPDATE ... SET} 从左到右求值，后面的表达式读到
 *       前面**已更新**的列值 → {@code start_time} 必须写在 {@code expire_time} 之前。
 *       顺序写反不报错，只会让 {@code start_time == expire_time}（续费后立即到期）。
 *       本类用"二次续费后 start ≈ 首期到期(+30天)"锁死这一点。</li>
 *   <li><b>受影响行数语义</b>：{@code renewCard} 以"0 行 = 无卡"作为插卡判据，
 *       依赖 Connector/J 默认的 matched-rows 语义（{@code useAffectedRows=false}）。
 *       本类第 6 例锁死"有卡必 &gt;0"。</li>
 *   <li><b>唯一键是否真的生效</b>：第 5 例直接撞 {@code uk_user_platform}。
 *       若失败，说明 <b>增量脚本 {@code 20260927-01} 没执行</b>（新建库走 DDL 则天然具备）。</li>
 *   <li><b>插入撞唯一键后同一事务内继续 UPDATE</b> 是否可行（MySQL 1062 是语句级错误，
 *       不像 PostgreSQL 会把事务标记为 aborted）—— 第 7 例验证。</li>
 * </ol>
 *
 * <p>测试数据用 {@code userId=999990001} 这类不会与真实数据冲突的取值，
 * 且类上 {@code @Transactional} → 默认回滚，不污染开发库。</p>
 *
 * @author moyun
 */
@SpringBootTest
@Transactional
class VipGrantCardDbTest {

    /** 测试专用 userId（远离真实数据） */
    private static final long UID = 999_990_001L;
    private static final String PLATFORM = "portal";
    private static final String TIER_MONTHLY = "monthly";
    private static final String TIER_PERMANENT = "permanent";

    /** 允许的执行耗时偏差（分钟）：断言用"约 N 天" */
    private static final long TOLERANCE_MINUTES = 5;

    @Autowired private IVipService vipService;
    @Autowired private VipUserCardMapper cardMapper;
    @Autowired private VipTierMapper tierMapper;

    private VipUserCard reload() {
        VipUserCard card = cardMapper.selectOne(new LambdaQueryWrapper<VipUserCard>()
                .eq(VipUserCard::getUserId, UID)
                .eq(VipUserCard::getPlatformCode, PLATFORM)
                .last("LIMIT 1"));
        assertNotNull(card, "会员卡应存在");
        return card;
    }

    /** 断言某时间点距现在约 N 天（负值表示过去） */
    private static void assertOffsetDays(LocalDateTime actual, long expectedDays, String what) {
        assertNotNull(actual, what + " 不应为 null");
        long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), actual);
        long expected = expectedDays * 24 * 60;
        assertTrue(Math.abs(minutes - expected) <= TOLERANCE_MINUTES,
                what + " 期望约 " + expectedDays + " 天，实际约 "
                        + Math.round(minutes / 60.0 / 24) + " 天（" + minutes + " 分钟）");
    }

    // ==================== ① 首购 + 二次续费：顺延语义与 SET 赋值顺序 ====================

    @Test
    @DisplayName("首购 30 天 → 再购 30 天：起点=首期到期(+30d)、到期=+60d（赋值顺序正确性的关键断言）")
    void firstPurchaseThenRenew_extendsFromFirstExpire() {
        vipService.grantCard(UID, PLATFORM, TIER_MONTHLY, 880001L);
        VipUserCard first = reload();
        assertOffsetDays(first.getStartTime(), 0, "首购生效时间");
        assertOffsetDays(first.getExpireTime(), 30, "首购到期时间");
        assertEquals(TIER_MONTHLY, first.getTierCode());
        assertEquals(880001L, first.getOrderId());
        assertEquals(1, first.getStatus());

        vipService.grantCard(UID, PLATFORM, TIER_MONTHLY, 880002L);
        VipUserCard second = reload();

        // ★ 若 SQL 里 start_time 写在 expire_time 之后，MySQL 会让 start_time 读到
        //   已更新的 expire_time → start 变成 +60 天，本条断言随即失败（且 expire 会继续膨胀）
        assertOffsetDays(second.getStartTime(), 30, "续费起点（应为首期到期时间，而非 NOW）");
        assertOffsetDays(second.getExpireTime(), 60, "续费到期时间（应为 +60 天）");
        assertEquals(880002L, second.getOrderId(), "应记录最近一次支付单");

        // 一端一卡：不新增行
        Long rows = cardMapper.selectCount(new LambdaQueryWrapper<VipUserCard>()
                .eq(VipUserCard::getUserId, UID)
                .eq(VipUserCard::getPlatformCode, PLATFORM));
        assertEquals(1L, rows, "同一 (user,platform) 必须恒只有一行");
    }

    // ==================== ② 已过期卡：从 NOW() 重算 ====================

    @Test
    @DisplayName("卡已过期 5 天 → 续费从 NOW() 重算（不把过期天数累加进去）")
    void expiredCard_renewsFromNow() {
        insertCard(LocalDateTime.now().minusDays(5), 1);

        vipService.grantCard(UID, PLATFORM, TIER_MONTHLY, 880003L);
        VipUserCard card = reload();

        assertOffsetDays(card.getStartTime(), 0, "过期卡续费起点（应为 NOW）");
        assertOffsetDays(card.getExpireTime(), 30, "过期卡续费到期时间（应为 NOW+30 天）");
    }

    // ==================== ③ 永久等级：expire_time 置 NULL ====================

    @Test
    @DisplayName("购买永久等级 → expire_time = NULL（含已有 30 天卡升级为永久）")
    void permanentTier_setsExpireNull() {
        vipService.grantCard(UID, PLATFORM, TIER_MONTHLY, 880004L);
        assertNotNull(reload().getExpireTime());

        vipService.grantCard(UID, PLATFORM, TIER_PERMANENT, 880005L);
        VipUserCard card = reload();

        assertNull(card.getExpireTime(), "永久等级必须把 expire_time 置 NULL");
        assertEquals(TIER_PERMANENT, card.getTierCode());
        assertOffsetDays(card.getStartTime(), 30, "升级为永久时起点应为原到期时间");
    }

    // ==================== ④ 有效天数为 null：明确报错（不是 NPE） ====================

    @Test
    @DisplayName("等级 duration_days 为 NULL → ServiceException（可操作文案），且不产生卡")
    void tierWithoutDuration_throwsServiceException() {
        String tierCode = "__test_no_duration_" + System.currentTimeMillis();
        VipTier bad = new VipTier();
        bad.setPlatformCode(PLATFORM);
        bad.setTierCode(tierCode);
        bad.setTierName("单测-无有效天数");
        bad.setDurationDays(null);
        bad.setStatus(1);
        tierMapper.insert(bad);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> vipService.grantCard(UID, PLATFORM, tierCode, 880006L));
        assertTrue(ex.getMessage().contains("未配置有效天数"), "实际: " + ex.getMessage());

        Long rows = cardMapper.selectCount(new LambdaQueryWrapper<VipUserCard>()
                .eq(VipUserCard::getUserId, UID)
                .eq(VipUserCard::getPlatformCode, PLATFORM));
        assertEquals(0L, rows, "报错时不应留下会员卡");
    }

    // ==================== ⑤ 唯一键真的生效（否则说明增量脚本没执行） ====================

    @Test
    @DisplayName("同 (user,platform) 第二次插入被 uk_user_platform 拦截")
    void uniqueKey_blocksSecondRow() {
        insertCard(LocalDateTime.now().plusDays(10), 1);

        VipUserCard dup = new VipUserCard();
        dup.setUserId(UID);
        dup.setPlatformCode(PLATFORM);
        dup.setTierCode(TIER_MONTHLY);
        dup.setStatus(1);
        dup.setExpireTime(LocalDateTime.now().plusDays(10));

        DuplicateKeyException ex = assertThrows(DuplicateKeyException.class,
                () -> cardMapper.insert(dup),
                "唯一键未生效：请确认已执行 increment-sql/20260927-01（新建库走 DDL 则天然具备）");
        assertTrue(String.valueOf(ex.getMessage()).contains("uk_user_platform"),
                "冲突的应是 uk_user_platform，实际: " + ex.getMessage());
    }

    // ==================== ⑥ "0 行 = 无卡" 判据在 JDBC 下成立 ====================

    @Test
    @DisplayName("renewCard：无卡返回 0；有卡返回 >0（且重复调用仍 >0，matched-rows 语义成立）")
    void renewCard_zeroMeansNoCard() {
        // 无卡
        int noCard = cardMapper.renewCard(UID, PLATFORM, TIER_MONTHLY, 880007L, 30);
        assertEquals(0, noCard, "无卡时必须返回 0（这是 grantCard 走插卡分支的判据）");

        insertCard(LocalDateTime.now().plusDays(10), 1);

        int first = cardMapper.renewCard(UID, PLATFORM, TIER_MONTHLY, 880008L, 30);
        assertTrue(first > 0, "有卡时必须 >0，否则会被误判为无卡而重复插卡（撞唯一键）");
        // 再调一次：即使业务值未变化，也必须 >0（Connector/J 默认 matched-rows）
        int second = cardMapper.renewCard(UID, PLATFORM, TIER_MONTHLY, 880008L, 30);
        assertTrue(second > 0, "重复续期仍须 >0（若为 0 说明 useAffectedRows 语义改变，判据失效）");
    }

    // ==================== ⑦ 1062 后同一事务内继续 UPDATE（MySQL 语句级错误） ====================

    @Test
    @DisplayName("插入撞唯一键后，同一事务内继续 UPDATE 成功（MySQL 1062 不废事务）")
    void duplicateKeyThenUpdateInSameTransaction_works() {
        insertCard(LocalDateTime.now().plusDays(10), 1);

        VipUserCard dup = new VipUserCard();
        dup.setUserId(UID);
        dup.setPlatformCode(PLATFORM);
        dup.setTierCode(TIER_MONTHLY);
        dup.setStatus(1);
        assertThrows(DuplicateKeyException.class, () -> cardMapper.insert(dup));

        // 关键：事务未被标记 aborted，后续语句照常执行
        int rows = cardMapper.renewCard(UID, PLATFORM, TIER_MONTHLY, 880009L, 30);
        assertTrue(rows > 0, "1062 之后继续 UPDATE 必须能命中（否则并发首购兜底路径不可用）");
        assertOffsetDays(reload().getExpireTime(), 40, "续期后到期时间应为 +40 天");
    }

    // ==================== 工具方法 ====================

    private void insertCard(LocalDateTime expireTime, int status) {
        VipUserCard card = new VipUserCard();
        card.setUserId(UID);
        card.setPlatformCode(PLATFORM);
        card.setTierCode(TIER_MONTHLY);
        card.setStartTime(LocalDateTime.now().minusDays(30));
        card.setExpireTime(expireTime);
        card.setStatus(status);
        cardMapper.insert(card);
    }
}
