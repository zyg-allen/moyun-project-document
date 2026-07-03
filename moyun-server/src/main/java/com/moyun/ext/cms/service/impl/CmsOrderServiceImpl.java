package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.service.ICmsOrderService;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * CMS 付费阅读订单后台管理 Service 实现（只读查询）
 *
 * <p>复用 {@link PortalTipOrderMapper}，列表走 selectPaidOrderListPage
 * （target_type='article_paid'，JOIN portal_user + portal_article）。</p>
 *
 * @author moyun
 */
@Service
public class CmsOrderServiceImpl implements ICmsOrderService {

    @Autowired
    private PortalTipOrderMapper tipOrderMapper;

    @Override
    public Page<PortalTipOrder> selectOrderPage(Page<PortalTipOrder> page, String status,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        return tipOrderMapper.selectPaidOrderListPage(page, status, startTime, endTime);
    }

    @Override
    public PortalTipOrder selectOrderById(Long id) {
        return tipOrderMapper.selectById(id);
    }
}
