package com.moyun.ledger.service;

import java.util.Map;

/**
 * 记账总览服务（首页 dashboard）
 *
 * @author moyun
 */
public interface ILedgerDashboardService {

    /**
     * 首页总览：净资产/总资产/总负债/涨跌标识（对比昨日快照）/本月收支/预算进度/最近20条流水
     *
     * @param userId 门户用户ID
     */
    Map<String, Object> dashboard(Long userId);
}
