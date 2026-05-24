import request from '@/utils/request'

/** 用户名密码登录,返回 LoginVO */
export function login(username, password) {
  return request.post('/v1/auth/login', { username, password })
}

/** 获取当前登录用户信息 */
export function me() {
  return request.get('/v1/auth/me')
}

/** 登出 */
export function logout() {
  return request.post('/v1/auth/logout')
}
