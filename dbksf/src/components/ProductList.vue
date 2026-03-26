<script setup>
import { formatCurrency, formatProductSummary } from '@/utils/format'

defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['view', 'report'])
</script>

<template>
  <section class="product-list" v-loading="loading">
    <el-empty
      v-if="!loading && !items.length"
      description="当前条件下暂无商品"
    />

    <article
      v-for="item in items"
      :key="item.id"
      class="product-row subtle-card"
    >
      <div class="product-cover">
        <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
        <span v-else>暂无图片</span>
      </div>

      <div class="product-main">
        <div class="product-head">
          <div>
            <h3>{{ item.name }}</h3>
            <span class="muted-text">{{ item.relativeTime }}</span>
          </div>

          <strong class="price-text">{{ formatCurrency(item.price) }}</strong>
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

        <p class="product-desc">{{ formatProductSummary(item.description) }}</p>
      </div>

      <div class="product-actions">
        <el-button type="primary" @click="$emit('view', item)">查看</el-button>
        <el-button plain @click="$emit('report', item)">举报</el-button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.product-list {
  display: grid;
  gap: 16px;
}

.product-row {
  display: grid;
  grid-template-columns: 164px minmax(0, 1fr) 120px;
  gap: 18px;
  padding: 18px;
  align-items: start;
}

.product-cover {
  display: grid;
  place-items: center;
  min-height: 138px;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(135deg, #eef5ee 0%, #ddeade 100%);
  color: var(--dbks-text-secondary);
}

.product-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-main {
  min-width: 0;
}

.product-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.product-head h3 {
  margin: 0 0 4px;
  font-size: 20px;
  line-height: 1.2;
}

.price-text {
  color: var(--dbks-accent-strong);
  white-space: nowrap;
  font-size: 18px;
}

.product-desc {
  margin: 16px 0 0;
  color: var(--dbks-text-secondary);
}

.product-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

@media (max-width: 920px) {
  .product-row {
    grid-template-columns: 1fr;
  }

  .product-cover {
    min-height: 220px;
  }

  .product-actions {
    flex-direction: row;
  }
}
</style>
