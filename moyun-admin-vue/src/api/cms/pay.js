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
