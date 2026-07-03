package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.LearnDashboardVO;

/**
 * 学习中心聚合 Service 接口（任务 3.1）
 *
 * @author moyun
 */
public interface ILearnCenterService {

    /**
     * 学习中心聚合数据（未登录返回概览统计；登录返回含个人计划、错题入口）
     *
     * @param currentUserId 当前登录用户ID（未登录传 null）
     */
    LearnDashboardVO getDashboard(Long currentUserId);
}
