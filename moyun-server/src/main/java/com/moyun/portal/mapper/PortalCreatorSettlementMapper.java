package com.moyun.portal.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalCreatorSettlement;

/**
 * 创作者分成结算 Mapper
 *
 * <p>收入聚合来源：portal_tip_order（status=paid，paid_time 在周期内）。
 * target_type 划分：
 *   article / column                 -> tip_income（打赏）
 *   article_paid                     -> paid_read_income（付费阅读）
 *   column_subscribe                 -> column_income（专栏订阅付费）</p>
 *
 * @author moyun
 */
@Mapper
public interface PortalCreatorSettlementMapper extends BaseMapper<PortalCreatorSettlement> {

    /**
     * 我的结算单分页（按 creator_id 过滤，倒序）
     * JOIN portal_user 取创作者昵称/头像
     */
    Page<PortalCreatorSettlement> selectMySettlementsPage(Page<PortalCreatorSettlement> page,
                                                          @Param("creatorId") Long creatorId);

    /**
     * 后台结算单分页（含创作者信息，支持按 period/status/creatorId 过滤）
     */
    Page<PortalCreatorSettlement> selectAdminPage(Page<PortalCreatorSettlement> page,
                                                   @Param("query") PortalCreatorSettlement query);

    /**
     * 结算单详情（含创作者信息）
     */
    PortalCreatorSettlement selectDetailById(@Param("id") Long id);

    /**
     * 校验某创作者某周期是否已存在结算单（uk_creator_period 兜底）
     */

    int countByCreatorAndPeriod(@Param("creatorId") Long creatorId, @Param("period") String period);

    /**
     * 聚合某创作者某周期的打赏收入（target_type=article/column，status=paid，paid_time 在区间内）
     */

    BigDecimal sumTipIncome(@Param("creatorId") Long creatorId,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合某创作者某周期的付费阅读收入（target_type=article_paid，status=paid，paid_time 在区间内）
     */

    BigDecimal sumPaidReadIncome(@Param("creatorId") Long creatorId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合某创作者某周期的专栏订阅收入（target_type=column_subscribe，status=paid，paid_time 在区间内）
     */

    BigDecimal sumColumnIncome(@Param("creatorId") Long creatorId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 月度生成时，聚合有收入的创作者ID列表（某周期内 portal_tip_order.author_id 去重）
     */

    List<Long> selectCreatorsWithIncome(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}
