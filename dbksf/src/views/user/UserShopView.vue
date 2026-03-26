<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ProductList from '@/components/ProductList.vue'
import ProductDetailDrawer from '@/components/ProductDetailDrawer.vue'
import ReportDialog from '@/components/ReportDialog.vue'
import { fetchProductDetail, fetchProductsByCategory, searchProducts } from '@/api/shop'
import { createReport } from '@/api/user'
import { CATEGORY_OPTIONS } from '@/utils/format'

const products = ref([])
const listLoading = ref(false)
const selectedCategory = ref(CATEGORY_OPTIONS[0])
const keyword = ref('')
const currentMode = ref('category')

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailProduct = ref(null)
const detailTags = ref([])

const reportVisible = ref(false)
const reportTarget = ref(null)
const reportSubmitting = ref(false)

const filterSummary = computed(() => {
  return currentMode.value === 'search'
    ? `搜索结果：${keyword.value.trim() || '未填写关键词'}`
    : `当前分类：${selectedCategory.value}`
})

async function loadByCategory(category = selectedCategory.value) {
  listLoading.value = true
  currentMode.value = 'category'

  try {
    products.value = (await fetchProductsByCategory(category)) || []
  } finally {
    listLoading.value = false
  }
}

async function handleCategoryChange(category) {
  selectedCategory.value = category
  keyword.value = ''
  await loadByCategory(category)
}

async function handleSearch() {
  const trimmedKeyword = keyword.value.trim()
  if (!trimmedKeyword) {
    await loadByCategory(selectedCategory.value)
    return
  }

  listLoading.value = true
  currentMode.value = 'search'

  try {
    products.value = (await searchProducts(trimmedKeyword)) || []
  } finally {
    listLoading.value = false
  }
}

async function handleView(item) {
  detailVisible.value = true
  detailLoading.value = true
  detailProduct.value = null
  detailTags.value = item.tags || []

  try {
    detailProduct.value = await fetchProductDetail(item.id)
  } catch (error) {
    detailProduct.value = null
  } finally {
    detailLoading.value = false
  }
}

function handleOpenReport(item) {
  reportTarget.value = item
  reportVisible.value = true
}

async function handleSubmitReport(reason) {
  if (!reportTarget.value) {
    return
  }

  reportSubmitting.value = true

  try {
    await createReport({
      productId: reportTarget.value.id,
      reason,
    })

    ElMessage.success('举报已提交')
    reportVisible.value = false
  } finally {
    reportSubmitting.value = false
  }
}

onMounted(() => {
  loadByCategory()
})
</script>

<template>
  <div class="shop-page">
    <section class="shop-hero section-card">
      <div>
        <span class="muted-text">商品浏览</span>
        <h1 class="page-title">按真实分类和关键词浏览在售商品</h1>
      </div>

      <div class="hero-actions">
        <div class="category-strip">
          <button
            v-for="category in CATEGORY_OPTIONS"
            :key="category"
            class="category-chip"
            :class="{ active: selectedCategory === category && currentMode === 'category' }"
            type="button"
            @click="handleCategoryChange(category)"
          >
            {{ category }}
          </button>
        </div>

        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="输入商品名称或标签关键词"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
      </div>

      <div class="hero-foot">
        <span class="filter-summary">{{ filterSummary }}</span>
        <span class="muted-text">当前后端没有“全部商品”接口，因此首页默认按“二手书”分类加载。</span>
      </div>
    </section>

    <section class="list-section section-card">
      <div class="section-head">
        <div>
          <h2 class="section-title">商品列表</h2>
          <p class="muted-text">支持查看详情与提交举报。举报成功后当前列表不会自动刷新。</p>
        </div>

        <el-button plain @click="loadByCategory(selectedCategory)">
          刷新当前分类
        </el-button>
      </div>

      <ProductList
        :items="products"
        :loading="listLoading"
        @view="handleView"
        @report="handleOpenReport"
      />
    </section>

    <ProductDetailDrawer
      v-model="detailVisible"
      :product="detailProduct"
      :tags="detailTags"
      :loading="detailLoading"
    />

    <ReportDialog
      v-model="reportVisible"
      :product-name="reportTarget?.name"
      :submitting="reportSubmitting"
      @submit="handleSubmitReport"
    />
  </div>
</template>

<style scoped>
.shop-page {
  display: grid;
  gap: 20px;
  padding: 28px;
}

.shop-hero,
.list-section {
  padding: 24px;
}

.shop-hero {
  display: grid;
  gap: 22px;
}

.hero-actions {
  display: grid;
  gap: 16px;
}

.category-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-chip {
  padding: 10px 16px;
  border: 1px solid var(--dbks-border);
  border-radius: 999px;
  background: var(--dbks-panel-strong);
  color: var(--dbks-text-secondary);
  cursor: pointer;
  transition: all 0.18s ease;
}

.category-chip.active {
  border-color: var(--dbks-accent);
  background: var(--dbks-accent-soft);
  color: var(--dbks-accent-strong);
}

.search-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.hero-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.filter-summary {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--dbks-accent-soft);
  color: var(--dbks-accent-strong);
  font-weight: 600;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.section-head p {
  margin: 8px 0 0;
}

@media (max-width: 720px) {
  .shop-page {
    padding: 16px;
  }

  .search-bar {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }
}
</style>
