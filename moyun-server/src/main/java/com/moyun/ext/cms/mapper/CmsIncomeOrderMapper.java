package com.moyun.ext.cms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 收入订单统一视图 Mapper（收入管理模块）
 *
 * <p>UNION ALL 合并业务订单来源（ledger_tip_order / portal_tip_order / pay_order biz_type='vip'），
 * 统一字段：platform / channel_code / status / pay_channel。统一会员 v12.0 后 VIP 订阅订单
 * 收敛到 pay_order（biz_type='vip'，platform 列 ledger→ledger_app / portal→portal，
 * 状态 PAID/SETTLED→paid、CLOSED→closed、CREATED→pending）。其余 pay_order 通道单据不参与（避免双算）。
 *
 * @author moyun
 */
@Mapper
public interface CmsIncomeOrderMapper {

    /**
     * 统一订单分页（动态条件：platform / status / 时间范围）
     */
    @Select("<script>" +
            "SELECT * FROM ( " +
            "  SELECT CONCAT('ledger_app-', t.id) AS order_key, 'ledger_app' AS platform, 'app_tip' AS channel_code, " +
            "         t.id AS order_id, t.user_id, t.amount, t.status, t.pay_channel, " +
            "         t.reason AS remark, t.target AS target_type, COALESCE(t.paid_time, t.create_time) AS pay_time " +
            "  FROM ledger_tip_order t " +
            "  UNION ALL " +
            "  SELECT CONCAT('portal-', o.id), 'portal', " +
            "         CASE WHEN o.target_type = 'article_paid' THEN 'paid_reading' ELSE 'portal_tip' END, " +
            "         o.id, o.user_id, o.amount, o.status, o.pay_channel, " +
            "         o.message, o.target_type, COALESCE(o.paid_time, o.created_time) " +
            "  FROM portal_tip_order o " +
            "  UNION ALL " +
            "  SELECT CONCAT('vip-', p.id), CASE WHEN p.platform = 'ledger' THEN 'ledger_app' ELSE 'portal' END, 'vip', " +
            "         p.id, p.user_id, p.amount, " +
            "         CASE WHEN p.status IN ('PAID', 'SETTLED') THEN 'paid' WHEN p.status = 'CLOSED' THEN 'closed' ELSE 'pending' END, " +
            "         p.channel, p.subject, " +
            "         SUBSTRING_INDEX(p.biz_no, ':', 2), COALESCE(p.pay_success_time, p.create_time) " +
            "  FROM pay_order p WHERE p.biz_type = 'vip' " +
            ") u " +
            "WHERE 1=1 " +
            "<if test='platform != null and platform != \"\"'> AND u.platform = #{platform} </if>" +
            "<if test='status != null and status != \"\"'> AND u.status = #{status} </if>" +
            "<if test='channelCode != null and channelCode != \"\"'> AND u.channel_code = #{channelCode} </if>" +
            "<if test='startTime != null'> AND u.pay_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND u.pay_time &lt;= #{endTime} </if>" +
            "ORDER BY u.pay_time DESC LIMIT #{offset}, #{size}" +
            "</script>")
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
    @Select("<script>" +
            "SELECT COUNT(*) FROM ( " +
            "  SELECT 'ledger_app' AS platform, 'app_tip' AS channel_code, t.status, t.pay_channel, t.create_time AS pay_time " +
            "  FROM ledger_tip_order t " +
            "  UNION ALL " +
            "  SELECT 'portal', CASE WHEN o.target_type = 'article_paid' THEN 'paid_reading' ELSE 'portal_tip' END, " +
            "         o.status, o.pay_channel, COALESCE(o.paid_time, o.created_time) " +
            "  FROM portal_tip_order o " +
            "  UNION ALL " +
            "  SELECT CASE WHEN p.platform = 'ledger' THEN 'ledger_app' ELSE 'portal' END, 'vip', " +
            "         CASE WHEN p.status IN ('PAID', 'SETTLED') THEN 'paid' WHEN p.status = 'CLOSED' THEN 'closed' ELSE 'pending' END, " +
            "         p.channel, COALESCE(p.pay_success_time, p.create_time) " +
            "  FROM pay_order p WHERE p.biz_type = 'vip' " +
            ") u " +
            "WHERE 1=1 " +
            "<if test='platform != null and platform != \"\"'> AND u.platform = #{platform} </if>" +
            "<if test='status != null and status != \"\"'> AND u.status = #{status} </if>" +
            "<if test='channelCode != null and channelCode != \"\"'> AND u.channel_code = #{channelCode} </if>" +
            "<if test='startTime != null'> AND u.pay_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND u.pay_time &lt;= #{endTime} </if>" +
            "</script>")
    long countIncomeOrders(@Param("platform") String platform,
                           @Param("channelCode") String channelCode,
                           @Param("status") String status,
                           @Param("startTime") java.time.LocalDateTime startTime,
                           @Param("endTime") java.time.LocalDateTime endTime);
}
