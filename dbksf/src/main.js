import { createApp } from 'vue'
import {
  ElAlert,
  ElAside,
  ElButton,
  ElCol,
  ElContainer,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDrawer,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElHeader,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElRow,
  ElSelect,
  ElSkeleton,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { clearSession, syncSessionFromStorage } from '@/composables/useSession'
import { bindUnauthorizedHandler } from '@/utils/request'

import './assets/main.css'

const elementComponents = [
  ElAlert,
  ElAside,
  ElButton,
  ElCol,
  ElContainer,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDrawer,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElHeader,
  ElInput,
  ElInputNumber,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElRow,
  ElSelect,
  ElSkeleton,
  ElTable,
  ElTableColumn,
  ElTag,
]

syncSessionFromStorage()

bindUnauthorizedHandler(() => {
  clearSession()

  const currentRoute = router.currentRoute.value
  if (currentRoute.path === '/login') {
    return
  }

  const nextRoute = { path: '/login' }
  if (currentRoute.fullPath && currentRoute.fullPath !== '/login') {
    nextRoute.query = { redirect: currentRoute.fullPath }
  }

  router.replace(nextRoute)
})

const app = createApp(App)

elementComponents.forEach((component) => {
  app.component(component.name, component)
})

app.use(ElLoading)
app.use(router)
app.mount('#app')
