package com.moyun.ext.cms.service;

import java.util.Map;

/**
 * 个人中心聚合 Dashboard 服务
 *
 * <p>聚合各模块（文章、收藏、书架、面试、面经、简历、关注、专栏、消息、通知、成长体系）
 * 的统计数字，用于个人中心 Tab 角标展示。</p>
 *
 * <p>设计原则：
 * <ul>
 *   <li>每个模块用单条 COUNT SQL，避免 N+1；</li>
 *   <li>优先复用各模块已有 Service / Mapper 方法；</li>
 *   <li>成长等级映射复用 {@code IPortalGrowthService} 的 LEVEL_THRESHOLDS 逻辑。</li>
 * </ul></p>
 *
 * @author moyun
 */
public interface IUserDashboardService {

    /**
     * 获取当前登录用户的个人中心聚合统计。
     *
     * @param userId 当前登录门户用户ID
     * @return 聚合统计数据 Map（字段名见接口文档约定）
     */
    Map<String, Object> getUserDashboard(Long userId);
}
