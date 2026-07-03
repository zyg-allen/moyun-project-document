package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalCreatorSettlement;

/**
 * 创作者分成结算 Service 接口（任务 4.7）
 *
 * <p>状态机：pending（待确认）-> confirmed（已确认）-> paid（已打款）。
 * 每位创作者每月唯一一条结算单。</p>
 *
 * @author moyun
 */
public interface ICreatorSettlementService {

    /**
     * 生成某周期的月度结算单（后台触发）。
     * 聚合该周期内所有有收入创作者的打赏/付费阅读/专栏订阅收入，
     * 按平台抽成比例计算 creator_income；已存在的结算单跳过（幂等）。
     *
     * @param period 结算周期，格式 yyyy-MM，如 2026-07；为 null 时默认上个月
     * @return 生成的结算单数量（已存在的不计入）
     */
    int generateMonthlySettlement(String period);

    /**
     * 我的结算单分页（前台创作者）
     */
    Page<PortalCreatorSettlement> mySettlements(Long creatorId, PageDomain query);

    /**
     * 后台结算单分页
     *
     * @param query 过滤条件（可含 creatorId / period / status）
     */
    Page<PortalCreatorSettlement> list(PortalCreatorSettlement query, PageDomain pageDomain);

    /**
     * 结算单详情（含创作者信息）
     */
    PortalCreatorSettlement detail(Long id);

    /**
     * 确认结算单（pending -> confirmed），仅后台
     */
    int confirm(Long id);

    /**
     * 标记已打款（confirmed -> paid），仅后台
     */
    int markPaid(Long id);
}
