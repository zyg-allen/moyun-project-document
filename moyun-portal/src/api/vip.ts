import { httpGet, httpPost } from './client';

/**
 * 门户端统一会员（v12.0：一端一套等级/权益，替代旧 interviewVip / resumeOptimizeVip）
 * 后端：PortalVipController → /portal/vip/*
 */

/** 权益项（等级内） */
export interface VipBenefitItemVO {
  code: string;
  name: string;
  /** 权益值：数字额度 或 'unlimited' */
  value: string;
  /** 计数周期：day/month/year/unlimited */
  period: string;
}

/** 会员等级（含该等级权益清单） */
export interface VipTierVO {
  tierCode: string;
  tierName: string;
  price: number;
  originalPrice: number | null;
  popular: boolean;
  /** -1 = 永久 */
  durationDays: number;
  description: string | null;
  benefits: VipBenefitItemVO[];
}

/** 我的会员详情（等级/到期/各权益已用剩余） */
export interface VipStatusVO {
  isVip: boolean;
  tierCode: string;
  tierName: string;
  expireTime: string | null;
  benefits: Array<VipBenefitItemVO & { used: number; left: number | null }>;
}

/** 订阅下单返回（收银台参数，前端跳 /pay/cashier） */
export interface VipSubscribeResult {
  amount: number;
  tierName: string;
  durationDays: number;
  payNo: string;
  codeUrl: string | null;
  expireTime: string | null;
  mockEnabled: boolean;
}

/** 等级与权益清单（售卖页展示） */
export const getVipTiers = () => {
  return httpGet<{ records: VipTierVO[] }>('/portal/vip/tiers');
};

/** 我的会员详情 */
export const getVipStatus = () => {
  return httpGet<VipStatusVO>('/portal/vip/status');
};

/** 订阅下单（tierCode 选档，clientUuid 幂等，返回收银台参数） */
export const subscribeVip = (data: { tierCode: string; clientUuid: string }) => {
  return httpPost<VipSubscribeResult>('/portal/vip/subscribe', data as unknown as Record<string, unknown>);
};

/** 从会员详情中取指定权益的剩余次数（unlimited 或查询失败返回 null） */
export function benefitLeft(status: VipStatusVO | null | undefined, benefitCode: string): number | null {
  const b = status?.benefits?.find((x) => x.code === benefitCode);
  return b ? b.left : null;
}
