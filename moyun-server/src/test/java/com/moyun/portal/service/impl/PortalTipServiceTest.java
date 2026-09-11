package com.moyun.portal.service.impl;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.mvc.handler.BusinessException;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.PayOrder;
import com.moyun.pay.gateway.IPayGateway;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.enums.PaymentChannel;
import com.moyun.portal.enums.PaymentStatus;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import com.moyun.portal.mapper.PortalUserGrowthMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.util.RealNameChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 打赏资金链路单测（v11.68 资金链路测试）
 *
 * <p>覆盖积分打赏 toggleTipOrList（@Transactional 核心链路：对象解析→实名校验→
 * 原子扣分 WHERE 防护→加对方分→落 PAID 订单→双方成长事件）与微信支付打赏
 * createWechatTipOrder（校验→pending 订单→网关下单→收银台参数）。
 * 支付回调闭环见 TipPayCallbackHandlerTest。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalTipServiceTest {

    @Mock private PortalTipOrderMapper tipOrderMapper;
    @Mock private PortalArticleMapper articleMapper;
    @Mock private PortalColumnMapper columnMapper;
    @Mock private PortalUserGrowthMapper growthMapper;
    @Mock private IPortalGrowthService growthService;
    @Mock private RealNameChecker realNameChecker;
    @Mock private IPayGateway payGateway;

    private PortalTipServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PortalTipServiceImpl();
        ReflectionTestUtils.setField(service, "portalTipOrderMapper", tipOrderMapper);
        ReflectionTestUtils.setField(service, "portalArticleMapper", articleMapper);
        ReflectionTestUtils.setField(service, "portalColumnMapper", columnMapper);
        ReflectionTestUtils.setField(service, "growthMapper", growthMapper);
        ReflectionTestUtils.setField(service, "portalGrowthService", growthService);
        ReflectionTestUtils.setField(service, "realNameChecker", realNameChecker);
        ReflectionTestUtils.setField(service, "payGateway", payGateway);
        // 积分打赏默认成功扣分（WHERE points >= delta 命中）
        when(growthMapper.deductPoints(anyLong(), anyInt())).thenReturn(1);
    }

    private PortalArticle article(Long authorId) {
        PortalArticle a = new PortalArticle();
        a.setId(10L);
        a.setAuthorId(authorId);
        return a;
    }

    private PortalTipOrder tipOrder(Long userId, String targetType, Long targetId, String amount) {
        PortalTipOrder o = new PortalTipOrder();
        o.setUserId(userId);
        o.setTargetType(targetType);
        o.setTargetId(targetId);
        o.setAmount(amount == null ? null : new BigDecimal(amount));
        return o;
    }

    // ================= 积分打赏 toggleTipOrList =================

    @Test
    @DisplayName("积分打赏成功：扣分+对方加分+订单 PAID/points+双方成长事件")
    void pointsTipHappyPath() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        PortalTipOrder result = service.toggleTipOrList(tipOrder(1L, "article", 10L, "50"));

        // 原子扣分（打赏者）+ 加分（作者）
        verify(growthMapper).deductPoints(1L, 50);
        verify(growthMapper).addPoints(2L, 50);
        // 订单：PAID + points 渠道 + 时间戳
        assertEquals(PaymentStatus.PAID.getCode(), result.getStatus());
        assertEquals(PaymentChannel.POINTS.getCode(), result.getPayMethod());
        assertEquals(2L, result.getAuthorId());
        assertNotNull(result.getPaidTime());
        verify(tipOrderMapper).insert(result);
        // 双方成长事件（被打赏者 receive_tip + 打赏者 tip_others）
        verify(growthService).recordEventWithTarget("article", "receive_tip", 2L, 1L, "article", 10L);
        verify(growthService).recordEvent("article", "tip_others", 1L, "article", 10L);
    }

    @Test
    @DisplayName("专栏打赏：column 类型解析专栏创建者为收款人")
    void columnTipResolvesColumnOwner() {
        PortalColumn column = new PortalColumn();
        column.setId(20L);
        column.setUserId(3L);
        when(columnMapper.selectById(20L)).thenReturn(column);

        PortalTipOrder result = service.toggleTipOrList(tipOrder(1L, "column", 20L, "10"));

        assertEquals(3L, result.getAuthorId());
        verify(growthMapper).deductPoints(1L, 10);
        verify(growthMapper).addPoints(3L, 10);
    }

    @Test
    @DisplayName("打赏对象不存在：文章查不到 → TIP_TARGET_NOT_FOUND")
    void targetNotFoundRejected() {
        when(articleMapper.selectPortalArticleById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "article", 99L, "50")));
        assertEquals("TIP_TARGET_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("未知 targetType：resolveAuthorId 返回 null → 拒绝")
    void unknownTargetTypeRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "video", 10L, "50")));
        assertEquals("TIP_TARGET_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("金额非法：null / 0 / 负数 → TIP_AMOUNT_INVALID")
    void invalidAmountRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        for (String amount : new String[]{null, "0", "-5"}) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.toggleTipOrList(tipOrder(1L, "article", 10L, amount)));
            assertEquals("TIP_AMOUNT_INVALID", ex.getCode());
        }
        verify(growthMapper, never()).deductPoints(anyLong(), anyInt());
    }

    @Test
    @DisplayName("积分非正整数：0.5 截断为 0 → TIP_AMOUNT_INVALID")
    void fractionalPointsBelowOneRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        // 0.5.intValue() = 0，正整数校验拦截
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "article", 10L, "0.5")));
        assertEquals("TIP_AMOUNT_INVALID", ex.getCode());
    }

    @Test
    @DisplayName("自赏拦截：打赏者与作者同人 → TIP_SELF_NOT_ALLOWED")
    void selfTipRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "article", 10L, "50")));
        assertEquals("TIP_SELF_NOT_ALLOWED", ex.getCode());
        verify(growthMapper, never()).deductPoints(anyLong(), anyInt());
    }

    @Test
    @DisplayName("积分不足：deductPoints WHERE 防护返回 0 → POINTS_INSUFFICIENT 且不加对方分")
    void insufficientPointsRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));
        when(growthMapper.deductPoints(1L, 500)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "article", 10L, "500")));
        assertEquals("POINTS_INSUFFICIENT", ex.getCode());
        // 关键资金安全断言：扣分失败后不得给作者加分、不得落订单
        verify(growthMapper, never()).addPoints(anyLong(), anyInt());
        verify(tipOrderMapper, never()).insert(any(PortalTipOrder.class));
    }

    @Test
    @DisplayName("付费阅读拦截：article_paid → PAYMENT_NOT_AVAILABLE（未扣费不发放权限）")
    void paidReadingBlocked() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "article_paid", 10L, "100")));
        assertEquals("PAYMENT_NOT_AVAILABLE", ex.getCode());
        verify(growthMapper, never()).deductPoints(anyLong(), anyInt());
        verify(tipOrderMapper, never()).insert(any(PortalTipOrder.class));
    }

    @Test
    @DisplayName("未实名：实名校验抛 ServiceException → 事务回滚（不扣分）")
    void realNameRequired() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));
        doThrow(new ServiceException("该操作需要先完成实名认证")).when(realNameChecker).checkRealName(1L);

        assertThrows(ServiceException.class,
                () -> service.toggleTipOrList(tipOrder(1L, "article", 10L, "50")));
        verify(growthMapper, never()).deductPoints(anyLong(), anyInt());
    }

    // ================= 微信支付打赏 createWechatTipOrder =================

    @Test
    @DisplayName("微信打赏成功：pending 订单 + 网关下单 + 收银台参数")
    void wechatTipHappyPath() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));
        PayOrder payOrder = new PayOrder();
        payOrder.setPayNo("PAY123");
        payOrder.setCodeUrl("wxp://code");
        payOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));
        when(payGateway.createOrder(eq("tip"), anyString(), eq("wechat"),
                eq(new BigDecimal("9.90")), anyString())).thenReturn(payOrder);
        PayProperties props = new PayProperties();
        // 默认 true（开发演示模式），显式置 false 断言生产语义透传
        props.getWechat().setMockEnabled(false);
        ReflectionTestUtils.setField(service, "payProperties", props);

        PortalTipOrder req = tipOrder(1L, "article", 10L, "9.90");
        var result = service.createWechatTipOrder(1L, req);

        // 订单落库：pending + wechat + 双方 ID 齐全
        ArgumentCaptor<PortalTipOrder> captor = ArgumentCaptor.forClass(PortalTipOrder.class);
        verify(tipOrderMapper).insert(captor.capture());
        PortalTipOrder saved = captor.getValue();
        assertEquals(PaymentStatus.PENDING.getCode(), saved.getStatus());
        assertEquals(PaymentChannel.WECHAT.getCode(), saved.getPayMethod());
        assertEquals(1L, saved.getUserId());
        assertEquals(2L, saved.getAuthorId());
        // 收银台参数
        assertEquals("PAY123", result.get("payNo"));
        assertEquals("wxp://code", result.get("codeUrl"));
        assertEquals(new BigDecimal("9.90"), result.get("amount"));
        assertEquals(false, result.get("mockEnabled"));
        // 不扣积分（微信通道走支付回调，不走积分账户）
        verify(growthMapper, never()).deductPoints(anyLong(), anyInt());
    }

    @Test
    @DisplayName("微信打赏金额超上限：>10000 元 → TIP_AMOUNT_INVALID")
    void wechatTipOverLimitRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createWechatTipOrder(1L, tipOrder(1L, "article", 10L, "10000.01")));
        assertEquals("TIP_AMOUNT_INVALID", ex.getCode());
        verify(tipOrderMapper, never()).insert(any(PortalTipOrder.class));
    }

    @Test
    @DisplayName("微信打赏金额非法：null / 0 / 负数 → TIP_AMOUNT_INVALID")
    void wechatTipInvalidAmountRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        for (String amount : new String[]{null, "0", "-1"}) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createWechatTipOrder(1L, tipOrder(1L, "article", 10L, amount)));
            assertEquals("TIP_AMOUNT_INVALID", ex.getCode());
        }
        verify(tipOrderMapper, never()).insert(any(PortalTipOrder.class));
    }

    @Test
    @DisplayName("微信打赏自赏拦截：与作者同人 → TIP_SELF_NOT_ALLOWED")
    void wechatSelfTipRejected() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createWechatTipOrder(1L, tipOrder(1L, "article", 10L, "5")));
        assertEquals("TIP_SELF_NOT_ALLOWED", ex.getCode());
    }

    @Test
    @DisplayName("微信打赏未登录：userId null → USER_NOT_LOGIN")
    void wechatTipNotLoggedInRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createWechatTipOrder(null, tipOrder(null, "article", 10L, "5")));
        assertEquals("USER_NOT_LOGIN", ex.getCode());
    }

    @Test
    @DisplayName("积分打赏边界：单笔 10000 积分在无上限校验下通过（微信通道才有 10000 元上限）")
    void pointsTipHasNoYuanLimit() {
        when(articleMapper.selectPortalArticleById(10L)).thenReturn(article(2L));

        PortalTipOrder result = service.toggleTipOrList(tipOrder(1L, "article", 10L, "10000"));

        // 积分通道无 10000 上限（与微信通道的元上限不同），只要积分够就通过
        verify(growthMapper).deductPoints(1L, 10000);
        assertEquals(PaymentStatus.PAID.getCode(), result.getStatus());
        assertTrue(result.getPaidTime() != null);
    }
}
