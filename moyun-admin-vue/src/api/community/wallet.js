import request from '@/utils/request'

export function listWallet(query) {
  return request({
    url: '/admin/wallet/page',
    method: 'get',
    params: query
  })
}

export function getWallet(walletId) {
  return request({
    url: `/admin/wallet/${walletId}`,
    method: 'get'
  })
}

export function listTransaction(query) {
  return request({
    url: '/admin/wallet/transaction/page',
    method: 'get',
    params: query
  })
}

export function listRecharge(query) {
  return request({
    url: '/admin/wallet/recharge/page',
    method: 'get',
    params: query
  })
}

export function listWithdraw(query) {
  return request({
    url: '/admin/wallet/withdraw/page',
    method: 'get',
    params: query
  })
}

export function approveWithdraw(withdrawId) {
  return request({
    url: `/admin/wallet/withdraw/approve/${withdrawId}`,
    method: 'post'
  })
}

export function rejectWithdraw(withdrawId, reason) {
  return request({
    url: `/admin/wallet/withdraw/reject/${withdrawId}`,
    method: 'post',
    data: { reason }
  })
}
