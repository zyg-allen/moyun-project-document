package com.moyun.vip.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.vip.domain.entity.VipTier;
import com.moyun.vip.domain.entity.VipUserCard;
import com.moyun.vip.mapper.VipTierMapper;
import com.moyun.vip.mapper.VipUserCardMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VIP 发卡（{@code VipServiceImpl.grantCard}）分支逻辑单测
 *
 * <p>为什么需要这组测试：该方法是**支付回调事务内**的发卡动作，
 * 出错的表现不是"报个错"，而是"<b>用户已付款、会员卡没发</b>"。</p>
 *
 * <p><b>覆盖边界说明</b>：本类只验证<b>分支走向</b>（续期 / 插卡 / 竞态兜底 / 校验失败），
 * 真实 SQL 的续期算术（顺延、过期重算、永久置 NULL、赋值顺序坑）由
 * {@code VipGrantCardDbTest} 在真实 MySQL 上验证——两处职责不重叠。</p>
 *
 * @author moyun
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VipServiceImplGrantCardTest {

    private static final String PLATFORM = "portal";

    @Mock private VipTierMapper tierMapper;
    @Mock private VipUserCardMapper cardMapper;

    @InjectMocks private VipServiceImpl vipService;

    @BeforeAll
    static void initTableInfo() {
        // 纯单测环境无 MyBatis-Plus 启动流程，LambdaQueryWrapper 的 lambda 列解析
        // 依赖实体 TableInfo 缓存，手动初始化（MP 官方单测做法）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, VipTier.class);
        TableInfoHelper.initTableInfo(assistant, VipUserCard.class);
    }

    private VipTier tier(Integer durationDays) {
        VipTier t = new VipTier();
        t.setPlatformCode(PLATFORM);
        t.setTierCode("monthly");
        t.setDurationDays(durationDays);
        return t;
    }

    // ==================== ① 有效天数缺失：明确报错，不是 NPE ====================

    @Test
    @DisplayName("durationDays=null → 抛可操作的 ServiceException（而非 NPE），且不写库")
    void durationDaysNull_throwsActionableServiceException() {
        when(tierMapper.selectOne(any())).thenReturn(tier(null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> vipService.grantCard(1L, PLATFORM, "monthly", 7001L));

        assertTrue(ex.getMessage().contains("未配置有效天数"),
                "错误文案必须让运营知道去后台补什么，实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(PLATFORM + "/monthly"),
                "错误文案必须带上定位信息（端/等级），实际: " + ex.getMessage());
        verify(cardMapper, never()).renewCard(any(), any(), any(), any(), any());
        verify(cardMapper, never()).insert(any(VipUserCard.class));
    }

    @Test
    @DisplayName("等级不存在 → 保持原有 ServiceException（回归守卫）")
    void tierNotFound_stillThrows() {
        when(tierMapper.selectOne(any())).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> vipService.grantCard(1L, PLATFORM, "nope", 7007L));
        assertTrue(ex.getMessage().contains("VIP等级不存在"));
        verify(cardMapper, never()).insert(any(VipUserCard.class));
    }

    // ==================== ② 已有卡：只续期，不再"读出来改再写回" ====================

    @Test
    @DisplayName("renewCard 命中 1 行 → 直接返回，绝不插入（消除 read-modify-write 丢更新）")
    void existingCard_renewsInPlace_noInsert() {
        when(tierMapper.selectOne(any())).thenReturn(tier(30));
        when(cardMapper.renewCard(any(), any(), any(), any(), any())).thenReturn(1);

        vipService.grantCard(1L, PLATFORM, "monthly", 7002L);

        verify(cardMapper).renewCard(1L, PLATFORM, "monthly", 7002L, 30);
        verify(cardMapper, never()).insert(any(VipUserCard.class));
        // 旧实现依赖 selectActiveCard + updateById，新实现不得再用（会重新引入丢更新）
        verify(cardMapper, never()).updateById(any(VipUserCard.class));
    }

    // ==================== ③ 无卡：插入首卡 ====================

    @Test
    @DisplayName("renewCard 命中 0 行（无卡）→ 插入首卡，到期 = now + 天数")
    void noCard_insertsFirstCard() {
        when(tierMapper.selectOne(any())).thenReturn(tier(30));
        when(cardMapper.renewCard(any(), any(), any(), any(), any())).thenReturn(0);

        LocalDateTime before = LocalDateTime.now();
        vipService.grantCard(1L, PLATFORM, "monthly", 7003L);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<VipUserCard> captor = ArgumentCaptor.forClass(VipUserCard.class);
        verify(cardMapper).insert(captor.capture());

        VipUserCard saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(PLATFORM, saved.getPlatformCode());
        assertEquals("monthly", saved.getTierCode());
        assertEquals(7003L, saved.getOrderId());
        assertEquals(1, saved.getStatus());
        assertNotNull(saved.getExpireTime());
        assertTrue(!saved.getExpireTime().isBefore(before.plusDays(30))
                        && !saved.getExpireTime().isAfter(after.plusDays(30)),
                "到期时间应为 now+30天，实际: " + saved.getExpireTime());
    }

    @Test
    @DisplayName("durationDays=-1（永久）且无卡 → 插入首卡，到期时间为 null")
    void noCard_permanentTier_expireTimeNull() {
        when(tierMapper.selectOne(any())).thenReturn(tier(-1));
        when(cardMapper.renewCard(any(), any(), any(), any(), any())).thenReturn(0);

        vipService.grantCard(1L, PLATFORM, "permanent", 7004L);

        ArgumentCaptor<VipUserCard> captor = ArgumentCaptor.forClass(VipUserCard.class);
        verify(cardMapper).insert(captor.capture());
        assertNull(captor.getValue().getExpireTime(), "永久等级到期时间必须为 null");
    }

    // ==================== ④ 并发首购竞态：唯一键兜底后转为续期 ====================

    @Test
    @DisplayName("并发首购：插入撞唯一键 → 转为续期一次，不抛异常（结果与串行执行一致）")
    void concurrentFirstPurchase_duplicateKeyFallsBackToRenew() {
        when(tierMapper.selectOne(any())).thenReturn(tier(30));
        // 第一次续期 0 行（以为无卡）→ 插入撞唯一键 → 第二次续期 1 行
        when(cardMapper.renewCard(any(), any(), any(), any(), any())).thenReturn(0, 1);
        when(cardMapper.insert(any(VipUserCard.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry '5-portal' for key 'uk_user_platform'"));

        vipService.grantCard(5L, PLATFORM, "monthly", 7005L);

        verify(cardMapper, times(2)).renewCard(5L, PLATFORM, "monthly", 7005L, 30);
        verify(cardMapper).insert(any(VipUserCard.class));
    }

    @Test
    @DisplayName("并发首购：撞唯一键但重试续期仍 0 行 → 明确报错（理论不可达的兜底）")
    void duplicateKey_thenRenewStillZero_throws() {
        when(tierMapper.selectOne(any())).thenReturn(tier(30));
        when(cardMapper.renewCard(any(), any(), any(), any(), any())).thenReturn(0);
        when(cardMapper.insert(any(VipUserCard.class)))
                .thenThrow(new DuplicateKeyException("uk_user_platform"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> vipService.grantCard(6L, PLATFORM, "monthly", 7006L));
        assertTrue(ex.getMessage().contains("唯一键冲突后未找到会员卡"),
                "必须留下可排查的明确错误，实际: " + ex.getMessage());
    }
}
