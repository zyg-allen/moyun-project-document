import { httpPost } from '@/api/client';
import type { ApiResponse } from '@/types/api';

/**
 * 发送短信验证码（V11.1）
 * POST /portal/sms/code/send
 * @param phone 手机号（接收人）
 * @param scene 场景：bankcard=银行卡绑定；member 预留会员开通
 */
export const sendSmsCode = (phone: string, scene: 'bankcard' | 'member') => {
  return httpPost<null>('/portal/sms/code/send', { phone, scene }) as Promise<ApiResponse<null>>;
};
