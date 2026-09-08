import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码（username 可选：全局开关关闭时，后端按账号风险判定——密码错误过多/长时间未登录——决定是否下发验证码）
export function getCodeImg(username) {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    params: username ? { username } : undefined,
    timeout: 20000
  })
}
