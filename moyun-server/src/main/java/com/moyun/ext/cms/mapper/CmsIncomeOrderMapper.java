package com.moyun.ext.cms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 收入订单统一视图 Mapper（收入管理模块）
 *
 * <p>UNION ALL 合并业务订单来源（ledger_tip_order / portal_tip_order / pay_order biz_type='vip'），
 * 统一字段：platform_code / channel_code / status / pay_channel。统一会员 v12.0 后 VIP 订阅订单
 * 收敛到 pay_order（biz_type='vip'，platform_code 列值为 portal/ledger，
 * 状态 PAID/SETTLED→paid、CLOSED→closed、CREATED→pending）。其余 pay_order 通道单据不参与（避免双算）。
 *
 * @author moyun
 */
@Mapper
public interface CmsIncomeOrderMapper {

    /**
     * 统一订单分页（动态条件：platform / status / 时间范围）
     */

    List<Map<String, Object>> selectIncomeOrders(@Param("platform") String platform,
                                                  @Param("channelCode") String channelCode,
                                                  @Param("status") String status,
                                                  @Param("startTime") java.time.LocalDateTime startTime,
                                                  @Param("endTime") java.time.LocalDateTime endTime,
                                                  @Param("offset") long offset,
                                                  @Param("size") long size);

    /**
     * 统一订单总数（同条件）
     */

    long countIncomeOrders(@Param("platform") String platform,
                           @Param("channelCode") String channelCode,
                           @Param("status") String status,
                           @Param("startTime") java.time.LocalDateTime startTime,
                           @Param("endTime") java.time.LocalDateTime endTime);
}
