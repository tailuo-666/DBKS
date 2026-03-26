import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSessionStorage, getStoredToken } from '@/utils/storage'

let unauthorizedHandler = null
let authNoticeTimer = null

function normalizeBackendMessage(message) {
  switch (message) {
    case 'forbidden':
      return '当前账号角色不匹配，无法访问所选端'
    case 'user not found':
      return '账号不存在'
    case 'verification code error':
      return '验证码错误'
    case 'wechat cannot be blank':
      return '微信号不能为空'
    case 'code cannot be blank':
      return '验证码不能为空'
    case 'create user failed':
      return '创建用户失败'
    default:
      return message
  }
}

function createHandledError(message, extra = {}) {
  const error = new Error(message)
  error.isHandled = true
  return Object.assign(error, extra)
}

function notifyAuthExpired() {
  if (authNoticeTimer) {
    return
  }

  ElMessage.warning('登录已失效，请重新登录')
  authNoticeTimer = window.setTimeout(() => {
    authNoticeTimer = null
  }, 600)
}

export function bindUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.authorization = token
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code === 1) {
        return payload.data
      }

      const message = normalizeBackendMessage(payload.msg) || '请求失败'
      ElMessage.error(message)
      return Promise.reject(createHandledError(message, { code: payload.code }))
    }

    return payload
  },
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      clearSessionStorage()
      notifyAuthExpired()
      if (typeof unauthorizedHandler === 'function') {
        unauthorizedHandler()
      }

      return Promise.reject(createHandledError('登录已失效，请重新登录', { status }))
    }

    if (status === 403) {
      const message = '无权限访问该页面'
      ElMessage.error(message)
      return Promise.reject(createHandledError(message, { status }))
    }

    const message = normalizeBackendMessage(error.response?.data?.msg)
      || error.message
      || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(createHandledError(message, { status }))
  },
)

export default request
