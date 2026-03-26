<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { clearSession, useSession } from '@/composables/useSession'

const route = useRoute()
const router = useRouter()
const { sessionState } = useSession()

const displayName = computed(() => sessionState.user?.username || '用户')
const currentTitle = computed(() => String(route.meta.title || '用户工作台'))

function handleLogout() {
  clearSession()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <div class="page-shell">
    <el-container class="shell-layout section-card">
      <el-aside class="shell-aside">
        <div class="brand-block">
          <span class="brand-kicker">DBKS Market</span>
          <h1>校园二手交易</h1>
          <p>浏览、发布并管理你自己的商品。</p>
        </div>

        <el-menu
          :default-active="route.path"
          class="shell-menu"
          router
        >
          <el-menu-item index="/user/shop">商品浏览</el-menu-item>
          <el-menu-item index="/user/publish">发布</el-menu-item>
          <el-menu-item index="/user/me">我的</el-menu-item>
        </el-menu>

        <div class="aside-footnote">
          <span class="muted-text">当前界面会严格按后端真实能力展示，不做分页与图片上传补充。</span>
        </div>
      </el-aside>

      <el-container class="shell-main">
        <el-header class="shell-header">
          <div>
            <span class="header-label">用户端</span>
            <h2>{{ currentTitle }}</h2>
          </div>

          <div class="header-actions">
            <el-tag effect="plain">{{ displayName }}</el-tag>
            <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
          </div>
        </el-header>

        <el-main class="shell-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.shell-layout {
  min-height: calc(100vh - 56px);
  overflow: hidden;
}

.shell-aside {
  display: flex;
  flex-direction: column;
  width: var(--dbks-sidebar-width);
  padding: 28px 20px;
  background:
    linear-gradient(180deg, rgba(45, 106, 67, 0.98) 0%, rgba(37, 84, 54, 0.96) 100%);
  color: #f7fbf7;
}

.brand-block {
  margin-bottom: 26px;
}

.brand-kicker {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.brand-block h1 {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.15;
}

.brand-block p {
  margin: 0;
  color: rgba(247, 251, 247, 0.78);
}

.shell-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(247, 251, 247, 0.8);
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-active-color: #ffffff;
  border-right: none;
  background: transparent;
}

.aside-footnote {
  margin-top: auto;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
  line-height: 1.5;
}

.aside-footnote .muted-text {
  color: rgba(247, 251, 247, 0.72);
}

.shell-main {
  min-width: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.54) 0%, rgba(247, 250, 246, 0.9) 100%);
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: auto;
  padding: 26px 28px 18px;
  border-bottom: 1px solid var(--dbks-border);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
}

.header-label {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--dbks-text-secondary);
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shell-header h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.03em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shell-content {
  padding: 0;
}

@media (max-width: 960px) {
  .shell-layout {
    display: block;
  }

  .shell-aside {
    width: 100%;
  }

  .shell-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
