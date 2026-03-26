<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchAdminProfile } from '@/api/auth'
import { saveSession, sessionState } from '@/composables/useSession'

const profile = ref(null)
const loading = ref(false)

const resolvedProfile = computed(() => ({
  id: profile.value?.id ?? sessionState.user?.id ?? null,
  username: profile.value?.username || sessionState.user?.username || '管理员账户',
  role: profile.value?.role || sessionState.user?.role || sessionState.role || '管理员',
}))

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
          <h1 class="page-title">{{ resolvedProfile.username }}</h1>
        </div>

        <el-tag type="danger" effect="plain">{{ resolvedProfile.role }}</el-tag>
      </div>

      <div class="profile-meta-grid">
        <div class="profile-meta-item">
          <span class="profile-meta-label">ID</span>
          <strong class="profile-meta-value">{{ resolvedProfile.id ?? '—' }}</strong>
        </div>
        <div class="profile-meta-item">
          <span class="profile-meta-label">用户名</span>
          <strong class="profile-meta-value">{{ resolvedProfile.username }}</strong>
        </div>
        <div class="profile-meta-item">
          <span class="profile-meta-label">角色</span>
          <strong class="profile-meta-value">{{ resolvedProfile.role }}</strong>
        </div>
      </div>

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

.profile-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.profile-meta-item {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid var(--dbks-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
}

.profile-meta-label {
  color: var(--dbks-text-secondary);
  font-size: 13px;
}

.profile-meta-value {
  font-size: 24px;
  line-height: 1.1;
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

  .profile-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
