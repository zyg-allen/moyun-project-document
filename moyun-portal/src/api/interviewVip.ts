import { httpGet, httpPost } from './client';

/**
 * 面试会员套餐（v11.82）
 * GET /portal/interview/vip/packages
 */
export interface InterviewVipPackageVO {
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
 * 我的会员状态（v11.82；v11.85 增加免费体验剩余次数）
 * GET /portal/interview/vip/status
 */
export interface InterviewVipStatusVO {
  isVip: boolean;
  vipExpire: string | null;
  /** v11.85：非会员剩余免费体验次数（语音面试每用户 2 次） */
  freeTrialLeft: number;
}

/**
 * 订阅下单返回（收银台参数，前端跳 /pay/cashier）
 */
export interface InterviewVipSubscribeResult {
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
 * 上架套餐列表（面试会员页展示，价格后台可配）
 */
export const getInterviewVipPackages = () => {
  return httpGet<{ records: InterviewVipPackageVO[] }>('/portal/interview/vip/packages');
};

/**
 * 我的会员状态（isVip/vipExpire）
 */
export const getInterviewVipStatus = () => {
  return httpGet<InterviewVipStatusVO>('/portal/interview/vip/status');
};

/**
 * 订阅下单（快照套餐信息，clientUuid 幂等，返回收银台参数）
 */
export const subscribeInterviewVip = (data: { packageId: number; clientUuid: string }) => {
  return httpPost<InterviewVipSubscribeResult>('/portal/interview/vip/subscribe', data as unknown as Record<string, unknown>);
};
