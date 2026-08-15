import request from '@/utils/request'

// 查询钱包列表
export function listWallet(query) {
  return request({
    url: '/cms/wallet/list',
    method: 'get',
    params: query
  })
}

// 查询钱包详情
export function getWallet(id) {
  return request({
    url: '/cms/wallet/' + id,
    method: 'get'
  })
}

// 查询交易流水列表
export function listTransaction(query) {
  return request({
    url: '/cms/wallet/transaction/list',
    method: 'get',
    params: query
  })
}

// 查询交易流水详情
export function getTransaction(id) {
  return request({
    url: '/cms/wallet/transaction/' + id,
    method: 'get'
  })
}
