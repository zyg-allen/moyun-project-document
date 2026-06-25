import request from '@/utils/request'

/**
 * 后台运营首页 API
 * 所有接口均需登录访问，数据 Redis 缓存5分钟
 */

// 获取运营首页完整数据（一次请求返回所有区块，推荐首屏使用）
export function getDashboardAll() {
  return request({
    url: '/system/dashboard/all',
    method: 'get'
  })
}

// 核心指标卡片
export function getDashboardMetrics() {
  return request({
    url: '/system/dashboard/metrics',
    method: 'get'
  })
}

// 今日统计
export function getDashboardToday() {
  return request({
    url: '/system/dashboard/today',
    method: 'get'
  })
}

// 近7天登录趋势
export function getLoginTrend() {
  return request({
    url: '/system/dashboard/login-trend',
    method: 'get'
  })
}

// 近7天文章发布趋势
export function getPublishTrend() {
  return request({
    url: '/system/dashboard/publish-trend',
    method: 'get'
  })
}

// 栏目排行榜
export function getCategoryRanking() {
  return request({
    url: '/system/dashboard/category-ranking',
    method: 'get'
  })
}

// 待办任务列表
export function getTodoTasks() {
  return request({
    url: '/system/dashboard/todo-tasks',
    method: 'get'
  })
}

// 与我相关任务（已办）
export function getMyTasks() {
  return request({
    url: '/system/dashboard/my-tasks',
    method: 'get'
  })
}

// 系统更新动态
export function getSystemActivities() {
  return request({
    url: '/system/dashboard/activities',
    method: 'get'
  })
}

// 热门文章 Top5
export function getHotArticles() {
  return request({
    url: '/system/dashboard/hot-articles',
    method: 'get'
  })
}

// 系统配置概览
export function getConfigOverview() {
  return request({
    url: '/system/dashboard/config-overview',
    method: 'get'
  })
}

// 手动刷新首页缓存
export function refreshDashboardCache() {
  return request({
    url: '/system/dashboard/refresh-cache',
    method: 'post'
  })
}
