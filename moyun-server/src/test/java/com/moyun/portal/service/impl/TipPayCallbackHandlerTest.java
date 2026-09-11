package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.INotificationService;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 打赏支付回调单测（v11.68 资金链路测试）
 *
 * <p>覆盖 TipPayCallbackHandler.onPaySuccess 闭环：pending→paid 条件更新幂等推进 →
 * 复式分账（平台抽成+作者所得，金额守恒）→ 双方站内通知。重点资金安全断言：
 * 非法状态拒绝推进、已支付幂等返回（渠道重试不重复分账）、并发竞争（条件更新
 * rows=0）抛异常整体回滚。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TipPayCallbackHandlerTest {

    @Mock private PortalTipOrderMapper tipOrderMapper;
    @Mock private ILedgerService ledgerService;
    @Mock private INotificationService notificationService;

    private TipPayCallbackHandler handler;

    @BeforeAll
    static void initTableInfo() {
        // 纯单测环境无 MyBatis-Plus 启动流程，LambdaUpdateWrapper 的 lambda 列解析
        // 依赖实体 TableInfo 缓存，手动初始化（MP 官方单测做法）
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), PortalTipOrder.class);
    }

    @BeforeEach
    void setUp() {
        handler = new TipPayCallbackHandler();
        ReflectionTestUtils.setField(handler, "tipOrderMapper", tipOrderMapper);
        ReflectionTestUtils.setField(handler, "ledgerService", ledgerService);
        ReflectionTestUtils.setField(handler, "notificationService", notificationService);
        // 默认条件更新命中 1 行
        when(tipOrderMapper.update(any(), any())).thenReturn(1);
    }

    private PayOrder payOrder(String payNo, String bizNo, String amountYuan) {
        PayOrder po = new PayOrder();
        po.setPayNo(payNo);
        po.setBizNo(bizNo);
        po.setAmount(new BigDecimal(amountYuan));
        return po;
    }

    private PortalTipOrder pendingTipOrder() {
        PortalTipOrder tip = new PortalTipOrder();
        tip.setId(500L);
        tip.setUserId(1L);
        tip.setAuthorId(2L);
        tip.setStatus("pending");
        tip.setTargetType("article");
        return tip;
    }

    private LedgerEntry entry(String role, String direction, String amount) {
        LedgerEntry e = new LedgerEntry();
        e.setAccountRole(role);
        e.setDirection(direction);
        e.setAmount(new BigDecimal(amount));
        return e;
    }

    @Test
    @DisplayName("回调成功：pending→paid 推进 + 复式分账 + 双方通知含金额明细")
    void paySuccessHappyPath() {
        PortalTipOrder tip = pendingTipOrder();
        when(tipOrderMapper.selectById(500L)).thenReturn(tip);
        // 分账：平台抽成 0.5 元 + 作者所得 9.5 元（金额守恒 10 元）
        when(ledgerService.settle(eq("PAY123"), eq("tip"), eq("500"),
                eq(new BigDecimal("10.00")), eq(2L), anyString()))
                .thenReturn(List.of(
                        entry(LedgerEntry.ROLE_PLATFORM, LedgerEntry.DIRECTION_CREDIT, "0.50"),
                        entry(LedgerEntry.ROLE_USER, LedgerEntry.DIRECTION_CREDIT, "9.50")));

        handler.onPaySuccess(payOrder("PAY123", "500", "10.00"));

        // 条件更新推进 paid
        verify(tipOrderMapper).update(any(), any());
        // 分账入参与金额一致
        verify(ledgerService).settle(eq("PAY123"), eq("tip"), eq("500"),
                eq(new BigDecimal("10.00")), eq(2L), eq("打赏"));
        // 双方通知：打赏者收支付成功、作者收到账含扣除服务费后金额
        ArgumentCaptor<String> contents = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(eq(1L), eq("pay"), eq("PAY123"),
                eq("打赏支付成功"), contents.capture());
        assertTrue(contents.getValue().contains("10.00"));
        verify(notificationService).send(eq(2L), eq("account"), eq("PAY123"),
                eq("收到一笔打赏"), contents.capture());
        assertTrue(contents.getValue().contains("9.50"), "作者通知应含实际到账金额 9.50");
    }

    @Test
    @DisplayName("打赏单不存在：抛 IllegalStateException（驱动网关重试/人工排查）")
    void tipOrderMissingThrows() {
        when(tipOrderMapper.selectById(999L)).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> handler.onPaySuccess(payOrder("PAY123", "999", "10.00")));
        verify(ledgerService, never()).settle(anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("幂等：已支付订单直接返回，不重复分账不重复通知")
    void alreadyPaidIdempotentReturn() {
        PortalTipOrder tip = pendingTipOrder();
        tip.setStatus("paid");
        when(tipOrderMapper.selectById(500L)).thenReturn(tip);

        handler.onPaySuccess(payOrder("PAY123", "500", "10.00"));

        // 渠道重试场景：不再推进状态、不再分账、不再通知
        verify(tipOrderMapper, never()).update(any(), any());
        verify(ledgerService, never()).settle(anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyLong(), anyString());
        verify(notificationService, never()).send(anyLong(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    @DisplayName("状态异常：非 pending 非 paid（如 closed）→ 抛 IllegalStateException")
    void abnormalStatusThrows() {
        PortalTipOrder tip = pendingTipOrder();
        tip.setStatus("closed");
        when(tipOrderMapper.selectById(500L)).thenReturn(tip);

        assertThrows(IllegalStateException.class,
                () -> handler.onPaySuccess(payOrder("PAY123", "500", "10.00")));
        verify(ledgerService, never()).settle(anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("并发竞争：条件更新 rows=0（已被并发处理）→ 抛 IllegalStateException 整体回滚")
    void concurrentUpdateReturnsZeroThrows() {
        when(tipOrderMapper.selectById(500L)).thenReturn(pendingTipOrder());
        // 另一线程已抢先推进状态，本次条件更新落空
        when(tipOrderMapper.update(any(), any())).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> handler.onPaySuccess(payOrder("PAY123", "500", "10.00")));
        // 关键资金安全断言：分账与通知不执行（事务回滚后由渠道重试驱动幂等分支）
        verify(ledgerService, never()).settle(anyString(), anyString(), anyString(),
                any(BigDecimal.class), anyLong(), anyString());
        verify(notificationService, never()).send(anyLong(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    @DisplayName("bizType 绑定：回调处理器注册为 tip 业务")
    void bizTypeIsTip() {
        assertEquals("tip", handler.bizType());
    }
}
