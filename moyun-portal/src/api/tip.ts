import { httpPost, httpGetList } from './client';
import type {
  PortalTipOrder,
  TipTargetType,
  TipTargetBody,
  TipQuery,
  Article,
} from '@/types/api';

/**
 * 发起打赏（需登录）
 * POST /portal/tip/{targetType}/{targetId}
 * targetType: article=文章打赏，column=专栏打赏
 */
export const tipTarget = (
  targetType: TipTargetType,
  targetId: string | number,
  data: TipTargetBody
) => {
  return httpPost<PortalTipOrder>(
    `/portal/tip/${targetType}/${targetId}`,
    data as unknown as Record<string, unknown>
  );
};

/**
 * 我打赏的（需登录，分页）
 * GET /portal/tip/my/given
 */
export const getMyGivenTips = (params?: TipQuery) => {
  return httpGetList<PortalTipOrder>('/portal/tip/my/given', params);
};

/**
 * 我收到的打赏（需登录，分页）
 * GET /portal/tip/my/received
 */
export const getMyReceivedTips = (params?: TipQuery) => {
  return httpGetList<PortalTipOrder>('/portal/tip/my/received', params);
};

/**
 * 目标的打赏列表（公开，分页）
 * GET /portal/tip/target/{targetType}/{targetId}
 */
export const getTargetTips = (
  targetType: TipTargetType,
  targetId: string | number,
  params?: TipQuery
) => {
  return httpGetList<PortalTipOrder>(
    `/portal/tip/target/${targetType}/${targetId}`,
    params
  );
};

/**
 * 购买付费阅读（需登录）
 * POST /portal/article/{id}/purchase
 * 复用打赏订单表，target_type='article_paid'
 */
export const purchaseArticle = (id: string | number) => {
  return httpPost<PortalTipOrder>(`/portal/article/${id}/purchase`);
};

/**
 * 我购买的文章（需登录，分页）
 * GET /portal/article/my/purchased
 */
export const getMyPurchasedArticles = (params?: TipQuery) => {
  return httpGetList<Article>('/portal/article/my/purchased', params);
};
