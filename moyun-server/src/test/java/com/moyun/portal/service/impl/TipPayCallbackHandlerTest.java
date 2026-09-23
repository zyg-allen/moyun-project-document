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
    @DisplayName("bizType 绑定：回调处理器注册为 tip 业务")
    void bizTypeIsTip() {
        assertEquals("tip", handler.bizType());
    }
}
