<script setup>
import { onMounted, ref } from 'vue'
import { fetchAdminProfile } from '@/api/auth'
import { saveSession, sessionState } from '@/composables/useSession'

const profile = ref(null)
const loading = ref(false)

async function loadProfile() {
  loading.value = true

  try {
    const nextProfile = await fetchAdminProfile()
    profile.value = nextProfile

    saveSession({
      token: sessionState.token,
      role: nextProfile.role,
      user: nextProfile,
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="admin-me-page">
    <section class="section-card info-card" v-loading="loading">
      <div class="card-head">
        <div>
          <span class="muted-text">管理员信息</span>
          <h1 class="page-title">{{ profile?.username || '管理员账户' }}</h1>
        </div>

        <el-tag type="danger" effect="plain">{{ profile?.role || '管理员' }}</el-tag>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="ID">
          {{ profile?.id || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ profile?.username || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ profile?.role || '—' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="subtle-card note-block">
        <strong>说明</strong>
        <p class="muted-text">
          当前管理端仅开放个人信息查看和举报审核能力，不扩展额外的后台配置页。
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-me-page {
  padding: 28px;
}

.info-card {
  display: grid;
  gap: 22px;
  padding: 24px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.note-block {
  padding: 18px;
}

.note-block p {
  margin: 8px 0 0;
}

@media (max-width: 720px) {
  .admin-me-page {
    padding: 16px;
  }

  .card-head {
    flex-direction: column;
  }
}
</style>
