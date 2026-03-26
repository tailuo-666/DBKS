<script setup>
import { computed, reactive, ref, watch } from 'vue'
import ImageUrlListInput from '@/components/ImageUrlListInput.vue'
import {
  CATEGORY_OPTIONS,
  EDITABLE_PRODUCT_STATUS_OPTIONS,
  PRODUCT_STATUS_REVIEWING,
  createEmptyProductDraft,
  normalizeImageUrls,
  normalizeTagList,
  serializeTags,
} from '@/utils/format'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  product: {
    type: Object,
    default: null,
  },
  missingFields: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  submitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const formRef = ref(null)
const form = reactive(createEmptyProductDraft())

const rules = {
  category: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  imageUrls: [
    {
      validator: (_rule, value, callback) => {
        if (!normalizeImageUrls(value).length) {
          callback(new Error('至少填写一条图片 URL'))
          return
        }

        callback()
      },
      trigger: 'change',
    },
  ],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [
    {
      validator: (_rule, value, callback) => {
        const amount = Number(value)
        if (!value && value !== 0) {
          callback(new Error('请输入价格'))
          return
        }

        if (!Number.isFinite(amount) || amount <= 0) {
          callback(new Error('价格必须大于 0'))
          return
        }

        callback()
      },
      trigger: 'blur',
    },
  ],
  status: [{ required: true, message: '请选择更新后的状态', trigger: 'change' }],
}

const requiresManualCompletion = computed(() => props.missingFields.length > 0)
const reviewingHint = computed(() => form.currentStatus === PRODUCT_STATUS_REVIEWING)

function syncForm() {
  const nextDraft = props.product
    ? createEmptyProductDraft({
        ...props.product,
        imageUrls: props.product.imageUrls?.length ? [...props.product.imageUrls] : [''],
        tags: normalizeTagList(props.product.tags),
      })
    : createEmptyProductDraft()

  Object.assign(form, nextDraft)
  formRef.value?.clearValidate()
}

watch(
  () => [props.modelValue, props.product],
  ([visible]) => {
    if (visible) {
      syncForm()
    }
  },
  { deep: true, immediate: true },
)

function closeDrawer() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }

  emit('submit', {
    category: form.category,
    name: form.name.trim(),
    imageUrls: normalizeImageUrls(form.imageUrls),
    description: form.description.trim(),
    price: Number(form.price),
    wechat: form.wechat?.trim() || '',
    address: form.address?.trim() || '',
    tags: serializeTags(form.tags),
    status: form.status,
  })
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    class="edit-drawer"
    size="560px"
    @close="closeDrawer"
  >
    <template #header>
      <div class="drawer-header">
        <span class="muted-text">编辑商品</span>
        <h3>{{ product?.name || '商品编辑' }}</h3>
      </div>
    </template>

    <div v-if="loading" class="drawer-loading">
      <el-skeleton animated :rows="12" />
    </div>

    <template v-else>
      <el-alert
        v-if="requiresManualCompletion"
        type="warning"
        :closable="false"
        show-icon
        class="edit-alert"
      >
        <template #title>
          当前商品没有完整详情接口可补齐，以下字段需要你手动确认：{{ missingFields.join('、') }}
        </template>
      </el-alert>

      <el-alert
        v-if="reviewingHint"
        type="info"
        :closable="false"
        class="edit-alert"
      >
        <template #title>
          当前商品状态为“审核中”，提交更新时必须重新选择“已上架”或“已下架”。
        </template>
      </el-alert>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="edit-form"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="商品分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类">
                <el-option
                  v-for="category in CATEGORY_OPTIONS"
                  :key="category"
                  :label="category"
                  :value="category"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="更新后状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态">
                <el-option
                  v-for="status in EDITABLE_PRODUCT_STATUS_OPTIONS"
                  :key="status"
                  :label="status"
                  :value="status"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="图片 URL 列表" prop="imageUrls">
          <ImageUrlListInput v-model="form.imageUrls" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="价格" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0.01"
                :precision="2"
                controls-position="right"
                class="full-width"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="联系微信">
              <el-input v-model="form.wechat" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="交易地点">
          <el-input v-model="form.address" maxlength="100" />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            collapse-tags
            collapse-tags-tooltip
            placeholder="输入标签后回车"
          >
            <el-option
              v-for="tag in form.tags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="closeDrawer">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          保存更新
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-header h3 {
  margin: 6px 0 0;
  font-size: 24px;
  line-height: 1.2;
}

.edit-alert + .edit-alert {
  margin-top: 12px;
}

.edit-form {
  margin-top: 18px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.full-width {
  width: 100%;
}
</style>
