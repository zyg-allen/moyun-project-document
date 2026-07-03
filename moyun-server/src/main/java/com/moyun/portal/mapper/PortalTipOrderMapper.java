package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalTipOrder;

/**
 * 打赏订单 Mapper（复用为付费阅读购买记录）
 *
 * @author moyun
 */
@Mapper
public interface PortalTipOrderMapper extends BaseMapper<PortalTipOrder> {

    /**
     * 我打赏的（user_id = userId）分页列表，JOIN portal_user 取被打赏者昵称/头像
     *
     * @param page   分页参数
     * @param userId 当前登录用户ID
     * @return 打赏订单分页列表
     */
    Page<PortalTipOrder> selectMyGivenPage(Page<PortalTipOrder> page, @Param("userId") Long userId);

    /**
     * 我收到的（author_id = userId）分页列表，JOIN portal_user 取打赏者昵称/头像
     *
     * @param page   分页参数
     * @param userId 当前登录用户ID
     * @return 打赏订单分页列表
     */
    Page<PortalTipOrder> selectMyReceivedPage(Page<PortalTipOrder> page, @Param("userId") Long userId);

    /**
     * 目标的打赏列表（公开，按 target_type + target_id 查询已支付记录）
     *
     * @param page       分页参数
     * @param targetType 目标类型 article/column/article_paid
     * @param targetId   目标ID
     * @return 打赏订单分页列表
     */
    Page<PortalTipOrder> selectTargetTipPage(Page<PortalTipOrder> page,
                                             @Param("targetType") String targetType,
                                             @Param("targetId") Long targetId);

    /**
     * 校验当前用户是否已对某目标支付过（target_type=article_paid，status=paid）
     * 用于付费阅读购买状态判断
     *
     * @param userId     用户ID
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 已支付记录数
     */
    int countPaidByUser(@Param("userId") Long userId,
                        @Param("targetType") String targetType,
                        @Param("targetId") Long targetId);

    /**
     * 后台打赏流水分页查询（JOIN portal_user 取打赏者/被打赏者昵称头像）
     * 支持按 targetType/status/时间范围筛选
     *
     * @param page       分页参数
     * @param targetType 目标类型 article/column/article_paid
     * @param status     状态 pending/paid/refunded
     * @param startTime  创建时间下限
     * @param endTime    创建时间上限
     * @return 打赏订单分页列表（含用户/作者信息）
     */
    Page<PortalTipOrder> selectAdminListPage(Page<PortalTipOrder> page,
                                              @Param("targetType") String targetType,
                                              @Param("status") String status,
                                              @Param("startTime") java.time.LocalDateTime startTime,
                                              @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 后台付费阅读订单分页查询（target_type='article_paid'，JOIN portal_user + portal_article）
     * 支持按 status/时间范围筛选，返回 PortalTipOrder（含 userNickname、targetTitle）
     *
     * @param page      分页参数
     * @param status    状态 pending/paid/refunded
     * @param startTime 创建时间下限
     * @param endTime   创建时间上限
     * @return 付费阅读订单分页列表
     */
    Page<PortalTipOrder> selectPaidOrderListPage(Page<PortalTipOrder> page,
                                                 @Param("status") String status,
                                                 @Param("startTime") java.time.LocalDateTime startTime,
                                                 @Param("endTime") java.time.LocalDateTime endTime);
}
