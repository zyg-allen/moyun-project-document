import { httpGet, httpPost } from './client';

/**
 * 简历优化会员套餐（v11.83）
 * GET /portal/resume/optimize/vip/packages
 */
export interface ResumeOptimizeVipPackageVO {
  id: number;
  name: string;
  price: number;
  originalPrice: number | null;
  durationDays: number;
  description: string | null;
  popular: boolean;
  sort: number;
  status: boolean;
}

/**
 * 我的会员状态（v11.83；v11.85 增加免费体验剩余次数）
 * GET /portal/resume/optimize/vip/status
 */
export interface ResumeOptimizeVipStatusVO {
  isVip: boolean;
  vipExpire: string | null;
  /** v11.85：非会员剩余免费体验次数（深度优化每用户 2 次） */
  freeTrialLeft: number;
}

/**
 * 订阅下单返回（收银台参数，前端跳 /pay/cashier）
 */
export interface ResumeOptimizeVipSubscribeResult {
  vipOrderId: number;
  amount: number;
  packageName: string;
  status: string;
  payNo: string;
  codeUrl: string | null;
  expireTime: string | null;
  mockEnabled: boolean;
}

/**
 * 上架套餐列表（简历优化会员页展示，价格后台可配）
 */
export const getResumeOptimizeVipPackages = () => {
  return httpGet<{ records: ResumeOptimizeVipPackageVO[] }>('/portal/resume/optimize/vip/packages');
};

/**
 * 我的会员状态（isVip/vipExpire）
 */
export const getResumeOptimizeVipStatus = () => {
  return httpGet<ResumeOptimizeVipStatusVO>('/portal/resume/optimize/vip/status');
};

/**
 * 订阅下单（快照套餐信息，clientUuid 幂等，返回收银台参数）
 */
export const subscribeResumeOptimizeVip = (data: { packageId: number; clientUuid: string }) => {
  return httpPost<ResumeOptimizeVipSubscribeResult>('/portal/resume/optimize/vip/subscribe', data as unknown as Record<string, unknown>);
};
