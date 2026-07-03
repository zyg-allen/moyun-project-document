package com.moyun.portal.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import com.moyun.portal.service.IPortalTipService;
import com.moyun.system.service.ISysConfigService;

/**
 * 打赏 业务实现（复用为付费阅读购买记录）
 * 简化实现：不接入真实支付，直接置 status='paid'；
 * 平台抽成比例读取 sys_config.platform_fee_rate，仅记录不实际转账。
 *
 * @author moyun
 */
@Service
public class PortalTipServiceImpl implements IPortalTipService {

    @Autowired
    private PortalTipOrderMapper portalTipOrderMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private PortalColumnMapper portalColumnMapper;

    @Autowired
    private ISysConfigService sysConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTipOrder toggleTipOrList(PortalTipOrder order) {
        // 1. 解析被打赏者 authorId（按 targetType 路由查询）
        Long authorId = resolveAuthorId(order.getTargetType(), order.getTargetId());
        if (authorId == null) {
            throw new RuntimeException("打赏对象不存在");
        }
        order.setAuthorId(authorId);

        // 2. 校验金额
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("打赏金额必须大于0");
        }

        // 3. 简化实现：直接置 status='paid'（不接入真实支付）
        order.setStatus("paid");
        order.setPayMethod("wallet");
        order.setPaidTime(LocalDateTime.now());
        order.setCreatedTime(LocalDateTime.now());

        // 4. 读取平台抽成比例配置（仅记录，不实际转账）
        // 平台抽成 = amount * platform_fee_rate，作者到账 = amount * (1 - platform_fee_rate)
        sysConfigService.selectConfigByKey("platform_fee_rate");

        // 5. 入库
        portalTipOrderMapper.insert(order);
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
