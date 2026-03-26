<script setup>
import { computed, ref, watch } from 'vue'
import { formatCurrency } from '@/utils/format'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  product: {
    type: Object,
    default: null,
  },
  tags: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const activeImage = ref('')

const imageUrls = computed(() => props.product?.imageUrls || [])

watch(
  imageUrls,
  (nextImages) => {
    activeImage.value = nextImages[0] || ''
  },
  { immediate: true },
)

function closeDrawer() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    class="detail-drawer"
    size="520px"
    @close="closeDrawer"
  >
    <template #header>
      <div class="drawer-header">
        <span class="muted-text">商品详情</span>
        <h3>{{ product?.name || '加载中' }}</h3>
      </div>
    </template>

    <div v-if="loading" class="drawer-loading">
      <el-skeleton animated :rows="10" />
    </div>

    <div v-else-if="product" class="detail-content">
      <div class="image-stage subtle-card">
        <img v-if="activeImage" :src="activeImage" :alt="product.name" />
        <span v-else>暂无图片</span>
      </div>

      <div class="thumb-grid" v-if="imageUrls.length > 1">
        <button
          v-for="url in imageUrls"
          :key="url"
          class="thumb-button"
          :class="{ active: url === activeImage }"
          type="button"
          @click="activeImage = url"
        >
          <img :src="url" :alt="product.name" />
        </button>
      </div>

      <div class="detail-meta">
        <div class="meta-row">
          <span class="meta-label">分类</span>
          <strong>{{ product.category }}</strong>
        </div>
        <div class="meta-row">
          <span class="meta-label">价格</span>
          <strong class="price-text">{{ formatCurrency(product.price) }}</strong>
        </div>
      </div>

      <div class="inline-tags" v-if="tags?.length">
        <el-tag
          v-for="tag in tags"
          :key="tag"
          effect="plain"
        >
          {{ tag }}
        </el-tag>
      </div>

      <div class="detail-block subtle-card">
        <span class="meta-label">商品描述</span>
        <p>{{ product.description || '暂无描述' }}</p>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="联系微信">
          {{ product.wechat || '未填写' }}
        </el-descriptions-item>
        <el-descriptions-item label="交易地点">
          {{ product.address || '未填写' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <el-empty v-else description="暂无可展示的商品详情" />
  </el-drawer>
</template>

<style scoped>
.drawer-header h3 {
  margin: 6px 0 0;
  font-size: 24px;
  line-height: 1.2;
}

.detail-content {
  display: grid;
  gap: 16px;
}

.image-stage {
  display: grid;
  place-items: center;
  min-height: 300px;
  overflow: hidden;
  padding: 8px;
}

.image-stage img {
  width: 100%;
  height: 100%;
  border-radius: 18px;
  object-fit: cover;
}

.thumb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(82px, 1fr));
  gap: 10px;
}

.thumb-button {
  padding: 0;
  border: 1px solid var(--dbks-border);
  border-radius: 14px;
  background: transparent;
  cursor: pointer;
  overflow: hidden;
}

.thumb-button.active {
  border-color: var(--dbks-accent);
  box-shadow: 0 0 0 2px var(--dbks-accent-soft);
}

.thumb-button img {
  width: 100%;
  height: 72px;
  object-fit: cover;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-row {
  padding: 14px 16px;
  border: 1px solid var(--dbks-border);
  border-radius: 18px;
  background: var(--dbks-panel-strong);
}

.meta-label {
  display: block;
  margin-bottom: 6px;
  color: var(--dbks-text-secondary);
  font-size: 13px;
}

.price-text {
  color: var(--dbks-accent-strong);
}

.detail-block {
  padding: 16px;
}

.detail-block p {
  margin: 10px 0 0;
  color: var(--dbks-text-secondary);
}

@media (max-width: 640px) {
  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
