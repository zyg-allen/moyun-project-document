package com.moyun.portal.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import com.moyun.portal.mapper.PortalUserGrowthMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalTipService;

/**
 * 打赏 业务实现
 *
 * MVP 阶段采用"积分打赏"，不接入真实支付通道：
 * - 打赏方扣减积分（portal_user_growth.points）
 * - 被打赏者获得积分
 * - 双方触发成长事件（receive_tip / tip_others），接入成长体系闭环
 * - 不涉及真实资金，规避支付资质门槛
 *
 * 付费阅读购买（target_type=article_paid）仍保留占位逻辑，待未来接入真实支付。
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalTipServiceImpl implements IPortalTipService {

    @Autowired
    private PortalTipOrderMapper portalTipOrderMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private PortalColumnMapper portalColumnMapper;

    @Autowired
    private PortalUserGrowthMapper growthMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTipOrder toggleTipOrList(PortalTipOrder order) {
        // 1. 解析被打赏者 authorId（按 targetType 路由查询）
        Long authorId = resolveAuthorId(order.getTargetType(), order.getTargetId());
        if (authorId == null) {
            throw new RuntimeException("打赏对象不存在");
        }
        order.setAuthorId(authorId);

        // 2. 校验金额/积分
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("打赏积分必须大于0");
        }

        // 付费阅读购买（article_paid）：拦截，待未来接入真实支付通道
        // 保留下方占位代码，但在进入前抛出友好错误，避免未扣费即发放付费阅读权限
        if ("article_paid".equals(order.getTargetType())) {
            throw new RuntimeException("付费阅读功能正在接入支付通道，暂不可用");
            // 以下占位逻辑保留，待支付通道接入后启用
            // order.setStatus("paid");
            // order.setPayMethod("wallet");
            // order.setPaidTime(LocalDateTime.now());
            // order.setCreatedTime(LocalDateTime.now());
            // portalTipOrderMapper.insert(order);
            // return order;
        }

        // 3. 积分打赏（article/column）：不涉及真实资金，用积分账户扣减
        Long tipperId = order.getUserId();
        if (tipperId == null) {
            throw new RuntimeException("请先登录");
        }
        int points = order.getAmount().intValue();
        if (points <= 0) {
            throw new RuntimeException("打赏积分必须为正整数");
        }
        // 不能给自己打赏
        if (tipperId != null && tipperId.equals(authorId)) {
            throw new RuntimeException("不能给自己打赏");
        }

        // 4. 确保双方成长记录存在
        growthMapper.insertIfNotExists(tipperId);
        growthMapper.insertIfNotExists(authorId);

        // 5. 原子扣减打赏者积分（deductPoints 带 points >= delta 条件，余额不足返回 0）
        int affected = growthMapper.deductPoints(tipperId, points);
        if (affected == 0) {
            throw new RuntimeException("积分余额不足，可通过签到或完成任务获取积分");
        }

        // 6. 给被打赏者加积分（创作鼓励，积分可在商城兑换）
        growthMapper.addPoints(authorId, points);

        // 7. 写订单：积分打赏直接置 paid，pay_method=points 区分
        order.setStatus("paid");
        order.setPayMethod("points");
        order.setPaidTime(LocalDateTime.now());
        order.setCreatedTime(LocalDateTime.now());
        portalTipOrderMapper.insert(order);

        // 8. 触发成长事件，接入激励闭环
        //    - 被打赏者：receive_tip（获得成长值，规则可在后台配置）
        //    - 打赏者：tip_others（记录打赏行为，可用于成就进度）
        //    recordEventWithTarget 内部规则未配置时安全返回 0，不抛异常
        portalGrowthService.recordEventWithTarget("article", "receive_tip", authorId, tipperId,
                order.getTargetType(), order.getTargetId());
        portalGrowthService.recordEvent("article", "tip_others", tipperId,
                order.getTargetType(), order.getTargetId());

        return order;
    }

    @Override
    public Page<PortalTipOrder> queryMyGiven(Long userId, Page<PortalTipOrder> page) {
        return portalTipOrderMapper.selectMyGivenPage(page, userId);
    }

    @Override
    public Page<PortalTipOrder> queryMyReceived(Long userId, Page<PortalTipOrder> page) {
        return portalTipOrderMapper.selectMyReceivedPage(page, userId);
    }

    @Override
    public Page<PortalTipOrder> queryTargetTips(String targetType, Long targetId, Page<PortalTipOrder> page) {
        return portalTipOrderMapper.selectTargetTipPage(page, targetType, targetId);
    }

    @Override
    public boolean hasPaid(Long userId, String targetType, Long targetId) {
        return portalTipOrderMapper.countPaidByUser(userId, targetType, targetId) > 0;
    }

    /**
     * 根据 targetType / targetId 解析被打赏者 authorId
     * - article / article_paid：取文章作者
     * - column：取专栏创建者
     */
    private Long resolveAuthorId(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return null;
        }
        switch (targetType) {
            case "article":
            case "article_paid": {
                PortalArticle article = portalArticleMapper.selectPortalArticleById(targetId);
                return article == null ? null : article.getAuthorId();
            }
            case "column": {
                PortalColumn column = portalColumnMapper.selectById(targetId);
                return column == null ? null : column.getUserId();
            }
            default:
                return null;
        }
    }
}
