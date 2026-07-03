package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalTipOrder;

import java.time.LocalDateTime;

/**
 * CMS 付费阅读订单后台管理 Service 接口（只读查询）
 *
 * <p>提供订单列表/详情（购买记录，含用户/文章/金额/状态）。</p>
 *
 * @author moyun
 */
public interface ICmsOrderService {

    /**
     * 后台付费阅读订单分页查询（target_type='article_paid'，含用户昵称、文章标题）
     */
    Page<PortalTipOrder> selectOrderPage(Page<PortalTipOrder> page, String status,
                                        LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据ID获取订单详情
     */
    PortalTipOrder selectOrderById(Long id);
}
