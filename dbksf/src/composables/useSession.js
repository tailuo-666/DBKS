import { computed, reactive, readonly } from 'vue'
import { fetchAdminProfile, fetchUserProfile } from '@/api/auth'
import { clearSessionStorage, loadSessionStorage, saveSessionStorage } from '@/utils/storage'
import { isAdminRole, isUserRole, normalizeRole, ROLE_ADMIN } from '@/utils/format'

function normalizeUser(user) {
  if (!user || typeof user !== 'object') {
    return null
  }

  return {
    ...user,
    role: normalizeRole(user.role),
  }
}

function createStateFromStorage() {
  const stored = loadSessionStorage()

  return {
    token: stored.token,
    role: normalizeRole(stored.role),
    user: normalizeUser(stored.user),
  }
}

function buildUserPayload(session) {
  if (session?.user) {
    return {
      id: session.user.id,
      username: session.user.username,
      role: normalizeRole(session.user.role || session.role || ''),
    }
  }

  if (session?.userId || session?.username) {
    return {
      id: session.userId || null,
      username: session.username || '',
      role: normalizeRole(session.role || ''),
    }
  }

  return null
}

function applySessionState(session) {
  sessionState.token = session?.token || ''
  sessionState.role = normalizeRole(session?.role || '')
  sessionState.user = normalizeUser(session?.user)
}

export const sessionState = reactive(createStateFromStorage())

export function syncSessionFromStorage() {
  applySessionState(createStateFromStorage())
}

export function saveSession(session) {
  const normalizedRole = normalizeRole(session?.role || session?.user?.role || '')
  const nextSession = {
    token: session?.token || '',
    role: normalizedRole,
    user: buildUserPayload({
      ...session,
      role: normalizedRole,
    }),
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
  const normalizedRoleHint = normalizeRole(roleHint)
  if (!token || !normalizedRoleHint) {
    throw new Error('\u65e0\u6cd5\u6062\u590d\u8eab\u4efd')
  }

  const fetcher = normalizedRoleHint === ROLE_ADMIN ? fetchAdminProfile : fetchUserProfile
  const profile = await fetcher()

  if (normalizeRole(profile?.role) !== normalizedRoleHint) {
    throw new Error('\u89d2\u8272\u4e0d\u5339\u914d')
  }

  saveSession({
    token,
    role: normalizeRole(profile.role),
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
    displayName: computed(() => sessionState.user?.username || '\u672a\u767b\u5f55'),
    isAdmin: computed(() => isAdminRole(sessionState.role)),
    isUser: computed(() => isUserRole(sessionState.role)),
    saveSession,
    clearSession,
    restoreIdentity,
    syncSessionFromStorage,
  }
}
