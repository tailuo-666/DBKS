<script setup>
import { reactive, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  productName: {
    type: String,
    default: '',
  },
  submitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const formRef = ref(null)
const form = reactive({
  reason: '',
})

const rules = {
  reason: [
    { required: true, message: '请输入举报原因', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      form.reason = ''
    }
  },
)

function closeDialog() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  const isValid = await formRef.value?.validate().catch(() => false)
  if (!isValid) {
    return
  }

  emit('submit', form.reason.trim())
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="提交举报"
    width="460px"
    destroy-on-close
    @close="closeDialog"
  >
    <el-alert
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        正在举报：{{ productName || '当前商品' }}
      </template>
    </el-alert>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="report-form"
    >
      <el-form-item label="举报原因" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="5"
          maxlength="200"
          show-word-limit
          placeholder="请简要说明举报原因，例如：疑似虚假商品、图片与实物不符等。"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交举报
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.report-form {
  margin-top: 18px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
