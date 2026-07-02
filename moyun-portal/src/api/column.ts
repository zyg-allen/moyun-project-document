import { httpGet, httpPost, httpPut, httpDelete, httpGetList } from './client';
import type {
  ColumnVO,
  ColumnListItemVO,
  ColumnQuery,
  ColumnSaveBody,
  SubscribeToggleResult,
  ColumnArticleSortItem,
} from '@/types/api';

/**
 * 专栏列表（公开，分页）
 * GET /portal/column/list
 */
export const getColumnList = (params?: ColumnQuery) => {
  return httpGetList<ColumnListItemVO>('/portal/column/list', params);
};

/**
 * 专栏详情（公开）
 * GET /portal/column/{id}
 */
export const getColumnDetail = (id: string | number) => {
  return httpGet<ColumnVO>(`/portal/column/${id}`);
};

/**
 * 创建/修改专栏（需登录）
 * POST /portal/column/save
 * 返回值为专栏ID
 */
export const saveColumn = (data: ColumnSaveBody) => {
  return httpPost<string | number>('/portal/column/save', data as unknown as Record<string, unknown>);
};

/**
 * 完结/恢复连载（需登录）
 * PUT /portal/column/{id}/finish
 */
export const toggleColumnFinish = (id: string | number) => {
  return httpPut<ColumnVO>(`/portal/column/${id}/finish`);
};

/**
 * 删除专栏（需登录）
 * DELETE /portal/column/{id}
 */
export const deleteColumn = (id: string | number) => {
  return httpDelete<number>(`/portal/column/${id}`);
};

/**
 * 切换订阅（需登录）
 * POST /portal/column/{id}/subscribe
 */
export const toggleSubscribe = (id: string | number) => {
  return httpPost<SubscribeToggleResult>(`/portal/column/${id}/subscribe`);
};

/**
 * 我创建的专栏（需登录，分页）
 * GET /portal/column/my/list
 */
export const getMyColumns = (params?: ColumnQuery) => {
  return httpGetList<ColumnListItemVO>('/portal/column/my/list', params);
};

/**
 * 我订阅的专栏（需登录，分页）
 * GET /portal/column/my/subscribed
 */
export const getSubscribedColumns = (params?: ColumnQuery) => {
  return httpGetList<ColumnListItemVO>('/portal/column/my/subscribed', params);
};

/**
 * 加入文章到专栏（需登录）
 * POST /portal/column/{columnId}/article/{articleId}
 */
export const addArticle = (columnId: string | number, articleId: string | number) => {
  return httpPost<number>(`/portal/column/${columnId}/article/${articleId}`);
};

/**
 * 从专栏移出文章（需登录）
 * DELETE /portal/column/{columnId}/article/{articleId}
 */
export const removeArticle = (columnId: string | number, articleId: string | number) => {
  return httpDelete<number>(`/portal/column/${columnId}/article/${articleId}`);
};

/**
 * 批量排序（需登录）
 * PUT /portal/column/{columnId}/sort
 * body: [{id, sortOrder}]
 */
export const sortArticles = (columnId: string | number, list: ColumnArticleSortItem[]) => {
  return httpPut<number>(
    `/portal/column/${columnId}/sort`,
    list as unknown as Record<string, unknown>
  );
};
