package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalTipOrder;

import java.time.LocalDateTime;

/**
 * CMS 打赏后台管理 Service 接口（只读查询）
 *
 * <p>提供打赏流水列表/详情，支持按 targetType/status/时间筛选。</p>
 *
 * @author moyun
 */
public interface ICmsTipService {

    /**
     * 后台打赏流水分页查询（含用户/作者信息）
     */
    Page<PortalTipOrder> selectTipPage(Page<PortalTipOrder> page, String targetType, String status,
                                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据ID获取打赏订单详情
     */
    PortalTipOrder selectTipById(Long id);
}
