import request from '@/utils/request'

export function sendLoginCode(wechat) {
  return request.post('/user/code', null, {
    params: { wechat },
  })
}

export function loginAsUser(payload) {
  return request.post('/user/login', payload)
}

export function loginAsAdmin(payload) {
  return request.post('/admin/login', payload)
}

export function fetchUserProfile(token) {
  return request.get('/user/me', {
    headers: token ? { authorization: token } : undefined,
  })
}

export function fetchAdminProfile(token) {
  return request.get('/admin/me', {
    headers: token ? { authorization: token } : undefined,
  })
}
