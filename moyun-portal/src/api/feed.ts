import { httpGetList } from './client';
import type { FeedEventVO } from '@/types/api';

// Feed 查询参数
// 注：pageNum 对应后端 PageDomain.pageNum（与 ColumnQuery 等保持一致）
export interface FeedQuery {
  pageNum?: number;
  pageSize?: number;
}

/**
 * 获取我关注的动态（需登录）
 * GET /portal/feed/following
 */
export const getFollowingFeed = (params?: FeedQuery) => {
  return httpGetList<FeedEventVO>('/portal/feed/following', params);
};

/**
 * 获取全站热门动态（公开）
 * GET /portal/feed/hot
 */
export const getHotFeed = (params?: FeedQuery) => {
  return httpGetList<FeedEventVO>('/portal/feed/hot', params);
};
