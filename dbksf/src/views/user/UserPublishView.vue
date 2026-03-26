<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ImageUrlListInput from '@/components/ImageUrlListInput.vue'
import { createProduct } from '@/api/user'
import {
  CATEGORY_OPTIONS,
  createEmptyProductDraft,
  normalizeImageUrls,
  serializeTags,
} from '@/utils/format'

const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const form = reactive(
  createEmptyProductDraft({
    category: CATEGORY_OPTIONS[0],
  }),
)

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
}

async function handleSubmit() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }

  submitting.value = true

  try {
    await createProduct({
      category: form.category,
      name: form.name.trim(),
      imageUrls: normalizeImageUrls(form.imageUrls),
      description: form.description.trim(),
      price: Number(form.price),
      wechat: form.wechat?.trim() || '',
      address: form.address?.trim() || '',
      tags: serializeTags(form.tags),
    })

    ElMessage.success('发布成功')
    router.push('/user/me')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="publish-page">
    <section class="publish-main section-card">
      <div class="section-head">
        <div>
          <span class="muted-text">商品发布</span>
          <h1 class="page-title">填写真实可提交的商品字段</h1>
        </div>

        <el-tag effect="plain">不支持本地图片上传</el-tag>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="publish-form"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="商品分类" prop="category">
              <el-select v-model="form.category">
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
            :rows="5"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="联系微信">
              <el-input v-model="form.wechat" maxlength="50" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="交易地点">
              <el-input v-model="form.address" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>

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

        <div class="submit-row">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交发布
          </el-button>
        </div>
      </el-form>
    </section>

    <aside class="publish-side">
      <section class="subtle-card side-card">
        <h2 class="section-title">发布前确认</h2>
        <ul class="hint-list">
          <li>图片字段必须填写 URL 字符串数组，后端不接受文件对象。</li>
          <li>标签会在提交前拼成空格分隔字符串，例如：`教材 九成新`。</li>
          <li>价格必须大于 0，商品发布后默认会进入“已上架”。</li>
        </ul>
      </section>

      <section class="subtle-card side-card">
        <h2 class="section-title">字段边界</h2>
        <p class="muted-text">
          当前实现不会补充分页、上传、草稿箱等后端尚未提供的能力。
        </p>
      </section>
    </aside>
  </div>
</template>

<style scoped>
.publish-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
  padding: 28px;
}

.publish-main,
.side-card {
  padding: 24px;
}

.publish-form {
  margin-top: 18px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.publish-side {
  display: grid;
  gap: 16px;
}

.hint-list {
  margin: 14px 0 0;
  padding-left: 18px;
  color: var(--dbks-text-secondary);
}

.hint-list li + li {
  margin-top: 10px;
}

.full-width {
  width: 100%;
}

@media (max-width: 1080px) {
  .publish-page {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .publish-page {
    padding: 16px;
  }

  .section-head {
    flex-direction: column;
  }
}
</style>
