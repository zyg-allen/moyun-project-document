package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalTipOrder;

/**
 * 打赏 业务层（复用为付费阅读购买记录）
 *
 * @author moyun
 */
public interface IPortalTipService {

    /**
     * 发起打赏（简化版：不接入真实支付，直接置 status='paid'）
     * 余额扣款通过 portal_wallet 完成，平台抽成比例读取 sys_config.platform_fee_rate（仅记录不实际转账）
     *
     * @param order 打赏订单（需含 targetType/targetId/amount，user_id/author_id 由调用方填充）
     * @return 创建后的打赏订单
     */
    PortalTipOrder toggleTipOrList(PortalTipOrder order);

    /**
     * 我打赏的（user_id = userId）
     *
     * @param userId 当前登录用户ID
     * @param page   分页参数
     * @return 打赏订单分页列表
     */
    Page<PortalTipOrder> queryMyGiven(Long userId, Page<PortalTipOrder> page);

    /**
     * 我收到的（author_id = userId）
     *
     * @param userId 当前登录用户ID
     * @param page   分页参数
     * @return 打赏订单分页列表
     */
    Page<PortalTipOrder> queryMyReceived(Long userId, Page<PortalTipOrder> page);

    /**
     * 目标的打赏列表（公开，仅返回 status='paid' 的记录）
     *
     * @param targetType 目标类型 article/column/article_paid
     * @param targetId   目标ID
     * @param page       分页参数
     * @return 打赏订单分页列表
     */
    Page<PortalTipOrder> queryTargetTips(String targetType, Long targetId, Page<PortalTipOrder> page);

    /**
     * 校验当前用户是否已对某目标支付过（用于付费阅读购买状态判断）
     *
     * @param userId     用户ID
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return true=已支付
     */
    boolean hasPaid(Long userId, String targetType, Long targetId);
}
