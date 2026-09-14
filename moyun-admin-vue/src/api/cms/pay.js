import request from '@/utils/request'

// ============ V11.0 支付中心 ============

// 支付订单分页（status/bizType/payNo 筛选）
export function listPayOrder(query) {
  return request({
    url: '/cms/pay/order/list',
    method: 'get',
    params: query
  })
}

// 支付订单详情
export function getPayOrder(payNo) {
  return request({
    url: '/cms/pay/order/' + payNo,
    method: 'get'
  })
}

// 手动关单（超时未支付订单）
export function closePayOrder(payNo) {
  return request({
    url: '/cms/pay/order/' + payNo + '/close',
    method: 'post'
  })
}

// 分账流水列表（视角：PLATFORM=平台 / USER=用户）
export function listPayLedger(query) {
  return request({
    url: '/cms/pay/ledger/list',
    method: 'get',
    params: query
  })
}

// 某支付单分账明细
export function getPayLedgerDetail(payNo) {
  return request({
    url: '/cms/pay/ledger/' + payNo + '/detail',
    method: 'get'
  })
}

// 分账汇总（平台总抽成 / 用户总收入）
export function payLedgerSummary() {
  return request({
    url: '/cms/pay/ledger/summary',
    method: 'get'
  })
}

// 用户银行卡列表（脱敏）
export function listBankCard(query) {
  return request({
    url: '/cms/pay/bank-card/list',
    method: 'get',
    params: query
  })
}

// 支付配置总览（脱敏）
export function getPayConfig() {
  return request({
    url: '/cms/pay/config/view',
    method: 'get'
  })
}

// 调整平台抽成费率
export function updateFeeRate(data) {
  return request({
    url: '/cms/pay/config/fee-rate',
    method: 'post',
    data: data
  })
}

// 收入总览（平台×渠道聚合，v11.78）
export function getRevenueOverview() {
  return request({
    url: '/cms/pay/revenue/overview',
    method: 'get'
  })
}

// ============ v11.79 收入管理模块（统一标准重构） ============

// 收入订单（全平台业务订单统一视图：ledger_tip_order + portal_tip_order 合并）
export function listIncomeOrder(query) {
  return request({
    url: '/cms/pay/income-order/list',
    method: 'get',
    params: query
  })
}

// 用户钱包账户列表（唯一钱包 pay_user_account + 昵称）
export function listWalletAccounts(query) {
  return request({
    url: '/cms/pay/wallet/accounts',
    method: 'get',
    params: query
  })
}

// 钱包守恒对账汇总（理论公账余额 = 平台抽成累计 + Σ用户余额）
export function walletSummary() {
  return request({
    url: '/cms/pay/wallet/summary',
    method: 'get'
  })
}

// 提现单列表（含审核中/已打款/已驳回汇总）
export function listWithdraw(query) {
  return request({
    url: '/cms/pay/withdraw/list',
    method: 'get',
    params: query
  })
}

// 提现审核通过（原子扣款 + 记账 + 出金）
export function passWithdraw(id) {
  return request({
    url: '/cms/pay/withdraw/' + id + '/pass',
    method: 'post'
  })
}

// 提现审核驳回（余额不动）
export function rejectWithdraw(id, reason) {
  return request({
    url: '/cms/pay/withdraw/' + id + '/reject',
    method: 'post',
    params: { reason }
  })
}
