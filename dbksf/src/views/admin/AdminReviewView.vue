<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchReports, handleReport } from '@/api/admin'
import {
  PRODUCT_STATUS_OFF_SHELF,
  PRODUCT_STATUS_ON_SHELF,
  REPORT_STATUS_PROCESSED,
  formatDateTime,
  mapStatusToTagType,
} from '@/utils/format'

const reports = ref([])
const loading = ref(false)
const processingId = ref(null)

async function loadReports() {
  loading.value = true

  try {
    reports.value = (await fetchReports()) || []
  } finally {
    loading.value = false
  }
}

async function handleReportAction(row, productStatus) {
  await ElMessageBox.confirm(
    `确认将举报 ${row.id} 对应商品处理为“${productStatus}”吗？`,
    '确认处理',
    {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    },
  )

  processingId.value = row.id

  try {
    await handleReport(row.id, { productStatus })
    ElMessage.success('举报处理完成')
    await loadReports()
  } finally {
    processingId.value = null
  }
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <div class="review-page">
    <section class="section-card review-head">
      <div>
        <span class="muted-text">举报审核</span>
        <h1 class="page-title">按待处理优先顺序审核举报记录</h1>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          当前后端只返回用户 ID、商品 ID、卖家 ID，不返回名称映射。
        </template>
      </el-alert>
    </section>

    <section class="section-card table-card">
      <div class="table-head">
        <div>
          <h2 class="section-title">举报列表</h2>
          <p class="muted-text">已处理的举报将禁用操作按钮；操作成功后会重新拉取整表。</p>
        </div>

        <el-button plain @click="loadReports">刷新列表</el-button>
      </div>

      <el-table :data="reports" v-loading="loading" border>
        <el-table-column prop="id" label="举报 ID" min-width="92" />
        <el-table-column prop="userId" label="举报人 ID" min-width="96" />
        <el-table-column prop="productId" label="商品 ID" min-width="96" />
        <el-table-column prop="sellerId" label="卖家 ID" min-width="96" />
        <el-table-column prop="reason" label="举报原因" min-width="220" show-overflow-tooltip />

        <el-table-column label="举报状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="mapStatusToTagType(row.status)" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="productName" label="商品名称" min-width="160">
          <template #default="{ row }">
            {{ row.productName || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="商品状态" min-width="110">
          <template #default="{ row }">
            <el-tag
              v-if="row.productStatus"
              :type="mapStatusToTagType(row.productStatus)"
              effect="plain"
            >
              {{ row.productStatus }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button
                size="small"
                type="success"
                plain
                :disabled="row.status === REPORT_STATUS_PROCESSED || processingId === row.id"
                :loading="processingId === row.id"
                @click="handleReportAction(row, PRODUCT_STATUS_ON_SHELF)"
              >
                上架
              </el-button>

              <el-button
                size="small"
                type="danger"
                plain
                :disabled="row.status === REPORT_STATUS_PROCESSED || processingId === row.id"
                :loading="processingId === row.id"
                @click="handleReportAction(row, PRODUCT_STATUS_OFF_SHELF)"
              >
                下架
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.review-page {
  display: grid;
  gap: 20px;
  padding: 28px;
}

.review-head,
.table-card {
  padding: 24px;
}

.review-head {
  display: grid;
  gap: 18px;
}

.table-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.table-head p {
  margin: 8px 0 0;
}

.action-row {
  display: flex;
  gap: 8px;
}

@media (max-width: 720px) {
  .review-page {
    padding: 16px;
  }

  .table-head {
    flex-direction: column;
  }
}
</style>
