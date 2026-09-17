package com.moyun.ext.cms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 收入订单统一视图 Mapper（v11.79 收入管理模块）
 *
 * <p>UNION ALL 合并五个业务订单表（ledger_tip_order / portal_tip_order / ledger_vip_order /
 * portal_interview_vip_order / portal_resume_optimize_order），统一字段：platform / channel_code /
 * status / pay_channel（v11.79 状态与渠道枚举统一后天然可合并；v11.81 新增 ledger_vip 记账VIP订阅；
 * v11.82 新增 interview_vip 面试会员订阅；v11.83 新增 resume_optimize 简历优化会员订阅，均为平台直收类）。
 * pay_order 为通道单据不参与（避免双算）。
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
            "  SELECT CONCAT('ledger_app-vip-', v.id), 'ledger_app', 'ledger_vip', " +
            "         v.id, v.user_id, v.amount, v.status, v.pay_channel, " +
            "         v.package_name, v.package_name, COALESCE(v.paid_time, v.create_time) " +
            "  FROM ledger_vip_order v " +
            "  UNION ALL " +
            "  SELECT CONCAT('portal-ivip-', i.id), 'portal', 'interview_vip', " +
            "         i.id, i.user_id, i.amount, i.status, i.pay_channel, " +
            "         i.package_name, i.package_name, COALESCE(i.paid_time, i.create_time) " +
            "  FROM portal_interview_vip_order i " +
            "  UNION ALL " +
            "  SELECT CONCAT('portal-rvip-', r.id), 'portal', 'resume_optimize', " +
            "         r.id, r.user_id, r.amount, r.status, r.pay_channel, " +
            "         r.package_name, r.package_name, COALESCE(r.paid_time, r.create_time) " +
            "  FROM portal_resume_optimize_order r " +
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
            "  SELECT 'ledger_app', 'ledger_vip', v.status, v.pay_channel, COALESCE(v.paid_time, v.create_time) " +
            "  FROM ledger_vip_order v " +
            "  UNION ALL " +
            "  SELECT 'portal', 'interview_vip', i.status, i.pay_channel, COALESCE(i.paid_time, i.create_time) " +
            "  FROM portal_interview_vip_order i " +
            "  UNION ALL " +
            "  SELECT 'portal', 'resume_optimize', r.status, r.pay_channel, COALESCE(r.paid_time, r.create_time) " +
            "  FROM portal_resume_optimize_order r " +
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
