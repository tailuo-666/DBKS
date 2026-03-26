import { computed, reactive, readonly } from 'vue'
import { fetchAdminProfile, fetchUserProfile } from '@/api/auth'
import { clearSessionStorage, loadSessionStorage, saveSessionStorage } from '@/utils/storage'
import { ROLE_ADMIN, ROLE_USER } from '@/utils/format'

function createStateFromStorage() {
  const stored = loadSessionStorage()

  return {
    token: stored.token,
    role: stored.role,
    user: stored.user,
  }
}

function buildUserPayload(session) {
  if (session?.user) {
    return {
      id: session.user.id,
      username: session.user.username,
      role: session.user.role || session.role || '',
    }
  }

  if (session?.userId || session?.username) {
    return {
      id: session.userId || null,
      username: session.username || '',
      role: session.role || '',
    }
  }

  return null
}

function applySessionState(session) {
  sessionState.token = session?.token || ''
  sessionState.role = session?.role || ''
  sessionState.user = session?.user ? { ...session.user } : null
}

export const sessionState = reactive(createStateFromStorage())

export function syncSessionFromStorage() {
  applySessionState(createStateFromStorage())
}

export function saveSession(session) {
  const nextSession = {
    token: session?.token || '',
    role: session?.role || '',
    user: buildUserPayload(session),
  }

  saveSessionStorage(nextSession)
  applySessionState(nextSession)
}

export function clearSession() {
  clearSessionStorage()
  applySessionState({
    token: '',
    role: '',
    user: null,
  })
}

export async function restoreIdentity(roleHint) {
  const token = sessionState.token || loadSessionStorage().token
  if (!token || !roleHint) {
    throw new Error('无法恢复身份')
  }

  const fetcher = roleHint === ROLE_ADMIN ? fetchAdminProfile : fetchUserProfile
  const profile = await fetcher()

  if (profile?.role !== roleHint) {
    throw new Error('角色不匹配')
  }

  saveSession({
    token,
    role: profile.role,
    userId: profile.id,
    username: profile.username,
    user: profile,
  })

  return profile
}

export function useSession() {
  return {
    sessionState: readonly(sessionState),
    isAuthenticated: computed(() => Boolean(sessionState.token)),
    displayName: computed(() => sessionState.user?.username || '未登录'),
    isAdmin: computed(() => sessionState.role === ROLE_ADMIN),
    isUser: computed(() => sessionState.role === ROLE_USER),
    saveSession,
    clearSession,
    restoreIdentity,
    syncSessionFromStorage,
  }
}
