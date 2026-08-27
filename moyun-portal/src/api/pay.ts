import { httpGet, httpPost } from './client';
import type {
  PayStatusResult,
  PayAccountOverview,
  PayLedgerEntry,
  PayLedgerListResult,
  PayNotification,
  PayNotificationListResult,
  UserBankCard,
  BankCardForm,
} from '@/types/api';

/**
 * 支付状态轮询（收银台 3s 轮询）
 * GET /portal/pay/status/{payNo}
 */
export const getPayStatus = (payNo: string) => {
  return httpGet<PayStatusResult>(`/portal/pay/status/${payNo}`);
};

/**
 * mock 模式模拟支付成功（触发与真实回调一致的后续链路）
 * POST /portal/pay/mock/{payNo}
 */
export const mockPaySuccess = (payNo: string) => {
  return httpPost<Record<string, unknown>>(`/portal/pay/mock/${payNo}`);
};

/**
 * 账户总览（余额/累计收入/累计提现）
 * GET /portal/pay/account/overview
 */
export const getAccountOverview = () => {
  return httpGet<PayAccountOverview>('/portal/pay/account/overview');
};

/**
 * 我的资金流水分页
 * GET /portal/pay/account/ledger
 */
export const getMyLedger = (params?: { current?: number; size?: number }) => {
  return httpGet<PayLedgerListResult>('/portal/pay/account/ledger', params);
};

/**
 * 我的银行卡列表（脱敏）
 * GET /portal/pay/bank-card/list
 */
export const getBankCards = () => {
  return httpGet<{ records: UserBankCard[]; total: number }>('/portal/pay/bank-card/list');
};

/**
 * 绑定银行卡
 * POST /portal/pay/bank-card
 */
export const bindBankCard = (data: BankCardForm) => {
  return httpPost<UserBankCard>('/portal/pay/bank-card', data as unknown as Record<string, unknown>);
};

/**
 * 删除银行卡
 * DELETE /portal/pay/bank-card/{cardId}
 */
export const deleteBankCard = (cardId: number | string) => {
  return import('./client').then((m) =>
    m.httpDelete<Record<string, unknown>>(`/portal/pay/bank-card/${cardId}`)
  );
};

/**
 * 设为默认银行卡
 * PUT /portal/pay/bank-card/{cardId}/default
 */
export const setDefaultBankCard = (cardId: number | string) => {
  return import('./client').then((m) =>
    m.httpPut<Record<string, unknown>>(`/portal/pay/bank-card/${cardId}/default`)
  );
};

/**
 * 我的支付通知分页（含未读数）
 * GET /portal/pay/notifications/list
 */
export const getPayNotifications = (params?: { current?: number; size?: number }) => {
  return httpGet<PayNotificationListResult>('/portal/pay/notifications/list', params);
};

/**
 * 未读通知数（Navbar 角标）
 * GET /portal/pay/notifications/unread-count
 */
export const getUnreadNotificationCount = () => {
  return httpGet<number>('/portal/pay/notifications/unread-count');
};

/**
 * 标记通知已读
 * POST /portal/pay/notifications/{id}/read
 */
export const markNotificationRead = (notificationId: number | string) => {
  return httpPost<Record<string, unknown>>(`/portal/pay/notifications/${notificationId}/read`);
};
