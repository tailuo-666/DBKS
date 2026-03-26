import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearSession, restoreIdentity, sessionState, syncSessionFromStorage } from '@/composables/useSession'
import { getDefaultRouteByRole, ROLE_ADMIN, ROLE_USER } from '@/utils/format'

function resolveHomeRoute() {
  syncSessionFromStorage()

  if (sessionState.role) {
    return getDefaultRouteByRole(sessionState.role)
  }

  return '/login'
}

function getRequiredRole(to) {
  const matchedRecord = [...to.matched].reverse().find((record) => record.meta.role)
  return matchedRecord?.meta.role || ''
}

const routes = [
  {
    path: '/',
    redirect: () => resolveHomeRoute(),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      title: '登录',
    },
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    meta: {
      requiresAuth: true,
      role: ROLE_USER,
    },
    children: [
      {
        path: '',
        redirect: '/user/shop',
      },
      {
        path: 'shop',
        name: 'user-shop',
        component: () => import('@/views/user/UserShopView.vue'),
        meta: {
          title: '商品浏览',
        },
      },
      {
        path: 'publish',
        name: 'user-publish',
        component: () => import('@/views/user/UserPublishView.vue'),
        meta: {
          title: '发布商品',
        },
      },
      {
        path: 'me',
        name: 'user-me',
        component: () => import('@/views/user/UserMeView.vue'),
        meta: {
          title: '我的商品',
        },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: {
      requiresAuth: true,
      role: ROLE_ADMIN,
    },
    children: [
      {
        path: '',
        redirect: '/admin/review',
      },
      {
        path: 'review',
        name: 'admin-review',
        component: () => import('@/views/admin/AdminReviewView.vue'),
        meta: {
          title: '举报审核',
        },
      },
      {
        path: 'me',
        name: 'admin-me',
        component: () => import('@/views/admin/AdminMeView.vue'),
        meta: {
          title: '管理员信息',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => resolveHomeRoute(),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  syncSessionFromStorage()

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiredRole = getRequiredRole(to)

  if (to.path === '/login') {
    if (sessionState.token && sessionState.role) {
      return getDefaultRouteByRole(sessionState.role)
    }

    return true
  }

  if (!requiresAuth) {
    return true
  }

  if (!sessionState.token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (!sessionState.role && requiredRole) {
    try {
      await restoreIdentity(requiredRole)
    } catch (error) {
      clearSession()
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }
  }

  if (!sessionState.role) {
    clearSession()
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (requiredRole && sessionState.role !== requiredRole) {
    ElMessage.warning('无权限访问该页面')
    return getDefaultRouteByRole(sessionState.role)
  }

  return true
})

router.afterEach((to) => {
  const pageTitle = to.meta.title ? `${to.meta.title} · DBKS` : 'DBKS 校园二手交易平台'
  document.title = pageTitle
})

export default router
