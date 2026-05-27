import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询门户用户列表
export function listUser(query) {
  return request({
    url: '/cms/user/list',
    method: 'get',
    params: query
  })
}

// 查询门户用户详细
export function getUser(id) {
  return request({
    url: '/cms/user/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增门户用户
export function addUser(data) {
  return request({
    url: '/cms/user',
    method: 'post',
    data: data
  })
}

// 修改门户用户
export function updateUser(data) {
  return request({
    url: '/cms/user',
    method: 'put',
    data: data
  })
}

// 删除门户用户
export function delUser(id) {
  return request({
    url: '/cms/user/' + id,
    method: 'delete'
  })
}

// 用户状态修改
export function changeUserStatus(id, status) {
  const data = {
    id,
    status
  }
  return request({
    url: '/cms/user/changeStatus',
    method: 'put',
    data: data
  })
}

// 用户密码重置
export function resetUserPwd(id, password) {
  const data = {
    id,
    password
  }
  return request({
    url: '/cms/user/resetPwd',
    method: 'put',
    data: data
  })
}
