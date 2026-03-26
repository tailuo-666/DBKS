<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchAdminProfile,
  fetchUserProfile,
  loginAsAdmin,
  loginAsUser,
  sendLoginCode,
} from '@/api/auth'
import { saveSession } from '@/composables/useSession'
import { ROLE_ADMIN, ROLE_USER, resolveSafeRedirect } from '@/utils/format'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const activeRole = ref(ROLE_USER)
const gettingCode = ref(false)
const submitting = ref(false)
const countdown = ref(0)

const form = reactive({
  wechat: '',
  code: '',
})

const loginModes = [
  {
    key: ROLE_USER,
    label: '用户端',
    description: '浏览商品、发布商品、管理自己的商品。',
  },
  {
    key: ROLE_ADMIN,
    label: '管理端',
    description: '查看举报记录并执行上架/下架处理。',
  },
]

const rules = {
  wechat: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const modeDescription = computed(() => {
  return loginModes.find((mode) => mode.key === activeRole.value)?.description || ''
})

let timerId = null

function stopCountdown() {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = null
  }
}

function startCountdown() {
  stopCountdown()
  countdown.value = 60

  timerId = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      stopCountdown()
      countdown.value = 0
    }
  }, 1000)
}

function switchMode(role) {
  activeRole.value = role
}

async function handleSendCode() {
  const wechat = form.wechat.trim()
  if (!wechat) {
    ElMessage.warning('请先输入微信号')
    return
  }

  gettingCode.value = true

  try {
    await sendLoginCode(wechat)
    startCountdown()
    ElMessage.success('验证码已发送，请查看后端日志')
  } finally {
    gettingCode.value = false
  }
}

async function handleLogin() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }

  submitting.value = true

  try {
    const payload = {
      wechat: form.wechat.trim(),
      code: form.code.trim(),
    }

    const token = activeRole.value === ROLE_ADMIN
      ? await loginAsAdmin(payload)
      : await loginAsUser(payload)

    const profile = activeRole.value === ROLE_ADMIN
      ? await fetchAdminProfile(token)
      : await fetchUserProfile(token)

    if (profile?.role !== activeRole.value) {
      throw new Error('登录角色与当前模式不一致')
    }

    saveSession({
      token,
      role: profile.role,
      user: profile,
    })

    ElMessage.success('登录成功')
    router.replace(resolveSafeRedirect(profile.role, route.query.redirect))
  } catch (error) {
    if (!error?.isHandled) {
      ElMessage.error(error?.message || '登录失败')
    }
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  stopCountdown()
})
</script>

<template>
  <div class="login-page">
    <section class="login-showcase">
      <div class="showcase-copy">
        <span class="showcase-kicker">DBKS Campus Market</span>
        <h1>校园二手交易平台</h1>
        <p>
          用一个统一入口连接用户端和管理端。界面只呈现当前后端真实支持的业务，不额外包装不存在的能力。
        </p>
      </div>

      <div class="showcase-points">
        <div class="point-card subtle-card">
          <strong>浏览与发布</strong>
          <span>按分类和关键词查找商品，直接发布带多条 URL 图片的商品信息。</span>
        </div>

        <div class="point-card subtle-card">
          <strong>严格鉴权</strong>
          <span>登录后立即通过 `/me` 恢复角色信息，用户端和管理端路由完全隔离。</span>
        </div>

        <div class="point-card subtle-card">
          <strong>审核透明</strong>
          <span>举报审核页只展示后端真实返回的字段，不假设额外昵称映射接口。</span>
        </div>
      </div>
    </section>

    <section class="login-panel section-card">
      <div class="panel-head">
        <span class="muted-text">统一登录入口</span>
        <h2>{{ activeRole }}</h2>
        <p>{{ modeDescription }}</p>
      </div>

      <div class="mode-switch">
        <button
          v-for="mode in loginModes"
          :key="mode.key"
          class="mode-button"
          :class="{ active: activeRole === mode.key }"
          type="button"
          @click="switchMode(mode.key)"
        >
          {{ mode.label }}
        </button>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
      >
        <el-form-item label="微信号" prop="wechat">
          <el-input
            v-model="form.wechat"
            placeholder="请输入用于登录的微信号"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              maxlength="12"
            />
            <el-button
              :disabled="countdown > 0"
              :loading="gettingCode"
              plain
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-button
          type="primary"
          class="submit-button"
          :loading="submitting"
          @click="handleLogin"
        >
          登录并进入{{ activeRole }}
        </el-button>
      </el-form>

      <div class="panel-note subtle-card">
        <strong>联调提示</strong>
        <span>管理员登录同样使用 `POST /user/code` 获取验证码，不存在单独的管理员发码接口。</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(380px, 420px);
  min-height: 100vh;
  padding: 28px;
  gap: 24px;
}

.login-showcase,
.login-panel {
  min-height: calc(100vh - 56px);
}

.login-showcase {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  border-radius: 32px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.76) 0%, rgba(243, 249, 242, 0.96) 100%);
  border: 1px solid var(--dbks-border);
  box-shadow: var(--dbks-shadow);
}

.showcase-kicker {
  display: inline-flex;
  margin-bottom: 18px;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--dbks-accent-soft);
  color: var(--dbks-accent-strong);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.showcase-copy h1 {
  margin: 0 0 18px;
  font-size: clamp(2.8rem, 4vw, 4.7rem);
  line-height: 0.98;
  letter-spacing: -0.05em;
}

.showcase-copy p {
  max-width: 580px;
  margin: 0;
  color: var(--dbks-text-secondary);
  font-size: 17px;
}

.showcase-points {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.point-card {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.point-card strong {
  font-size: 18px;
}

.point-card span {
  color: var(--dbks-text-secondary);
}

.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 32px;
}

.panel-head h2 {
  margin: 6px 0 10px;
  font-size: 34px;
  line-height: 1.05;
}

.panel-head p {
  margin: 0;
  color: var(--dbks-text-secondary);
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 24px 0;
  padding: 6px;
  border-radius: 18px;
  background: var(--dbks-panel-muted);
}

.mode-button {
  padding: 12px 14px;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: var(--dbks-text-secondary);
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.mode-button.active {
  background: var(--dbks-panel-strong);
  color: var(--dbks-accent-strong);
  box-shadow: 0 12px 30px rgba(29, 53, 38, 0.08);
}

.login-form {
  margin-top: 4px;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
  height: 46px;
}

.panel-note {
  display: grid;
  gap: 8px;
  margin-top: 24px;
  padding: 16px;
}

.panel-note span {
  color: var(--dbks-text-secondary);
}

@media (max-width: 1080px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-showcase,
  .login-panel {
    min-height: auto;
  }

  .showcase-points {
    grid-template-columns: 1fr;
    margin-top: 24px;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-showcase,
  .login-panel {
    padding: 20px;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
