const TOKEN_KEY = 'dbks_token'
const ROLE_KEY = 'dbks_role'
const USER_KEY = 'dbks_user'

function isBrowser() {
  return typeof window !== 'undefined'
}

function safeParseUser(rawValue) {
  if (!rawValue) {
    return null
  }

  try {
    const parsed = JSON.parse(rawValue)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch (error) {
    return null
  }
}

export function loadSessionStorage() {
  if (!isBrowser()) {
    return {
      token: '',
      role: '',
      user: null,
    }
  }

  return {
    token: window.localStorage.getItem(TOKEN_KEY) || '',
    role: window.localStorage.getItem(ROLE_KEY) || '',
    user: safeParseUser(window.localStorage.getItem(USER_KEY)),
  }
}

export function getStoredToken() {
  return loadSessionStorage().token
}

export function getStoredRole() {
  return loadSessionStorage().role
}

export function getStoredUser() {
  return loadSessionStorage().user
}

export function saveSessionStorage(session) {
  if (!isBrowser()) {
    return
  }

  const token = session?.token || ''
  const role = session?.role || ''
  const user = session?.user || null

  if (token) {
    window.localStorage.setItem(TOKEN_KEY, token)
  } else {
    window.localStorage.removeItem(TOKEN_KEY)
  }

  if (role) {
    window.localStorage.setItem(ROLE_KEY, role)
  } else {
    window.localStorage.removeItem(ROLE_KEY)
  }

  if (user) {
    window.localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    window.localStorage.removeItem(USER_KEY)
  }
}

export function clearSessionStorage() {
  if (!isBrowser()) {
    return
  }

  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(ROLE_KEY)
  window.localStorage.removeItem(USER_KEY)
}
