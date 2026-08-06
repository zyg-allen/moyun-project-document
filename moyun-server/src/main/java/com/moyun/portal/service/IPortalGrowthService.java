package com.moyun.portal.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyun.portal.domain.vo.AchievementVO;
import com.moyun.portal.domain.vo.GrowthTimelineVO;
import com.moyun.portal.domain.vo.UserBadgeVO;
import com.moyun.portal.domain.vo.UserGrowthVO;
import com.moyun.portal.domain.vo.UserStatsVO;

import java.util.List;

/**
 * 门户用户成长体系 服务层
 *
 * 提供统一的成长值记录、统计聚合、成就检测能力，
 * 供文章、读书空间、面试空间三大模块调用。
 *
 * @author moyun
 */
public interface IPortalGrowthService {

    /**
     * 记录成长事件（同步，需在调用方事务内执行）
     *
     * @param module      模块: article/reading/interview/all
     * @param action      行为编码（对应 portal_growth_rule.action）
     * @param userId      获得成长值的用户ID
     * @param entityType  实体类型（可为 null）
     * @param entityId    实体ID（可为 null）
     * @return 实际获得的成长值（0 表示因每日上限未获得）
     */
    int recordEvent(String module, String action, Long userId, String entityType, Long entityId);

    /**
     * 记录成长事件（带目标用户，用于"被赞/被收藏/被关注"场景）
     *
     * @param module        模块
     * @param action        行为编码
     * @param userId        获得成长值的用户ID
     * @param targetUserId  目标用户ID（操作者，可为 null）
     * @param entityType    实体类型
     * @param entityId      实体ID
     * @return 实际获得的成长值
     */
    int recordEventWithTarget(String module, String action, Long userId, Long targetUserId, String entityType, Long entityId);

    /**
     * 扣减成长值（用于审核驳回等回滚场景）
     * <p>原子扣减，带下界保护（growth_value 不会变为负数）。
     * 同步扣减赛季值，并重新计算等级。
     *
     * @param userId 用户ID
     * @param delta  扣减的成长值（正数）
     * @return 实际扣减的成长值，0 表示余额不足或无成长记录
     */
    int deductGrowth(Long userId, int delta);

    /**
     * 按实体回滚成长值（审核驳回等场景）
     * <p>查询该用户在该实体上获得的成长值流水，按当初实际获得的 growthDelta 精确扣减。
     * 已扣减过的实体不重复扣减（通过插入负向流水实现幂等）。
     *
     * @param module      模块
     * @param action      原行为编码（如 publish_article）
     * @param userId      用户ID
     * @param entityType  实体类型（如 article）
     * @param entityId    实体ID
     * @param rollbackAction 回滚流水 action 标识（如 publish_article_rollback），用于幂等去重
     * @return 实际扣减的成长值，0 表示无原始流水或已回滚过
     */
    int deductGrowthForEntity(String module, String action, Long userId,
                              String entityType, Long entityId, String rollbackAction);

    /**
     * 获取用户成长信息
     */
    UserGrowthVO getUserGrowth(Long userId);

    /**
     * 获取用户统计信息
     */
    UserStatsVO getUserStats(Long userId);

    /**
     * 获取用户徽章列表
     */
    List<UserBadgeVO> getUserBadges(Long userId);

    /**
     * 获取所有成就（含当前用户达成状态）
     *
     * @param userId 当前用户ID（可为 null，未登录时 earned 恒为 false）
     */
    List<AchievementVO> getAllAchievements(Long userId);

    /**
     * 获取赛季排行榜
     *
     * @param limit 前N名
     */
    List<UserGrowthVO> getRanking(int limit);

    /**
     * 用户签到
     *
     * @param userId 用户ID
     * @return 签到结果信息
     */
    java.util.Map<String, Object> checkin(Long userId);

    /**
     * 检测并授予成就（内部方法，可在关键操作后调用）
     *
     * @param userId 用户ID
     */
    void checkAndGrantAchievements(Long userId);

    /**
     * 获取用户成长时间线（分页，支持按模块筛选）
     * 统一聚合读书、面试、创作等各模块的行为记录
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param module 模块筛选（article/reading/interview/all），为空或 all 表示全部
     * @return 时间线分页数据（含关联标题、封面等展示字段）
     */
    IPage<GrowthTimelineVO> getTimeline(IPage<GrowthTimelineVO> page, Long userId, String module);
}
