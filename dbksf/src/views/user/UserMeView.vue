<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ProductEditDrawer from '@/components/ProductEditDrawer.vue'
import { fetchUserProfile } from '@/api/auth'
import { fetchProductDetail } from '@/api/shop'
import { fetchMyProducts, updateProduct } from '@/api/user'
import { saveSession, sessionState } from '@/composables/useSession'
import {
  PRODUCT_STATUS_ON_SHELF,
  buildDraftFromMineItem,
  buildDraftFromPublishedProduct,
  mapStatusToTagType,
} from '@/utils/format'

const profile = ref(null)
const products = ref([])
const pageLoading = ref(false)
const productsLoading = ref(false)

const editVisible = ref(false)
const editLoading = ref(false)
const editSubmitting = ref(false)
const editDraft = ref(null)
const editTarget = ref(null)
const missingFields = ref([])

async function refreshProducts() {
  productsLoading.value = true

  try {
    products.value = (await fetchMyProducts()) || []
  } finally {
    productsLoading.value = false
  }
}

async function loadPage() {
  pageLoading.value = true

  try {
    const [nextProfile, nextProducts] = await Promise.all([
      fetchUserProfile(),
      fetchMyProducts(),
    ])

    profile.value = nextProfile
    products.value = nextProducts || []

    saveSession({
      token: sessionState.token,
      role: nextProfile.role,
      user: nextProfile,
    })
  } finally {
    pageLoading.value = false
  }
}

async function handleEdit(item) {
  editVisible.value = true
  editLoading.value = true
  editTarget.value = item
  editDraft.value = null
  missingFields.value = []

  const manualFields = item.imageUrl
    ? ['分类', '价格', '联系微信', '交易地点', '更多图片 URL（如有）']
    : ['分类', '价格', '联系微信', '交易地点', '至少一条图片 URL']

  try {
    if (item.status === PRODUCT_STATUS_ON_SHELF) {
      try {
        const detail = await fetchProductDetail(item.id)
        editDraft.value = buildDraftFromPublishedProduct(item, detail)
      } catch (error) {
        editDraft.value = buildDraftFromMineItem(item)
        missingFields.value = manualFields
      }
    } else {
      editDraft.value = buildDraftFromMineItem(item)
      missingFields.value = manualFields
    }
  } finally {
    editLoading.value = false
  }
}

async function handleSubmitEdit(payload) {
  if (!editTarget.value) {
    return
  }

  editSubmitting.value = true

  try {
    await updateProduct(editTarget.value.id, payload)
    ElMessage.success('商品已更新')
    editVisible.value = false
    await refreshProducts()
  } finally {
    editSubmitting.value = false
  }
}

onMounted(() => {
  loadPage()
})
</script>

<template>
  <div class="me-page" v-loading="pageLoading">
    <section class="profile-card section-card">
      <div class="profile-head">
        <div>
          <span class="muted-text">用户信息</span>
          <h1 class="page-title">{{ profile?.username || '当前用户' }}</h1>
        </div>

        <el-tag effect="plain">{{ profile?.role || '用户端' }}</el-tag>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="用户 ID">
          {{ profile?.id || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ profile?.username || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ profile?.role || '—' }}
        </el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="products-card section-card">
      <div class="section-head">
        <div>
          <h2 class="section-title">我的商品</h2>
          <p class="muted-text">编辑时会按后端真实能力决定哪些字段能自动补齐，哪些字段需要手动补填。</p>
        </div>

        <el-button plain @click="refreshProducts">刷新列表</el-button>
      </div>

      <el-empty
        v-if="!productsLoading && !products.length"
        description="你还没有发布过商品"
      />

      <div v-else class="my-product-list" v-loading="productsLoading">
        <article
          v-for="item in products"
          :key="item.id"
          class="my-product-row subtle-card"
        >
          <div class="cover-box">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
            <span v-else>暂无图片</span>
          </div>

          <div class="content-box">
            <div class="content-head">
              <div>
                <h3>{{ item.name }}</h3>
                <el-tag :type="mapStatusToTagType(item.status)" effect="plain">
                  {{ item.status }}
                </el-tag>
              </div>

              <el-button type="primary" plain @click="handleEdit(item)">
                编辑
              </el-button>
            </div>

            <div class="inline-tags" v-if="item.tags?.length">
              <el-tag
                v-for="tag in item.tags"
                :key="tag"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </div>

            <p class="muted-text">{{ item.description || '暂无描述' }}</p>
          </div>
        </article>
      </div>
    </section>

    <ProductEditDrawer
      v-model="editVisible"
      :product="editDraft"
      :missing-fields="missingFields"
      :loading="editLoading"
      :submitting="editSubmitting"
      @submit="handleSubmitEdit"
    />
  </div>
</template>

<style scoped>
.me-page {
  display: grid;
  gap: 20px;
  padding: 28px;
}

.profile-card,
.products-card {
  padding: 24px;
}

.profile-head,
.section-head,
.content-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.section-head {
  margin-bottom: 18px;
}

.section-head p {
  margin: 8px 0 0;
}

.my-product-list {
  display: grid;
  gap: 16px;
}

.my-product-row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
}

.cover-box {
  display: grid;
  place-items: center;
  min-height: 126px;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(135deg, #edf5ee 0%, #dbe7dd 100%);
  color: var(--dbks-text-secondary);
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content-box {
  min-width: 0;
}

.content-head h3 {
  margin: 0 0 10px;
  font-size: 20px;
}

.content-box p {
  margin: 14px 0 0;
}

@media (max-width: 720px) {
  .me-page {
    padding: 16px;
  }

  .profile-head,
  .section-head,
  .content-head {
    flex-direction: column;
  }

  .my-product-row {
    grid-template-columns: 1fr;
  }
}
</style>
