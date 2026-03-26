<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [''],
  },
})

const emit = defineEmits(['update:modelValue'])

function getUrls() {
  return Array.isArray(props.modelValue) && props.modelValue.length
    ? [...props.modelValue]
    : ['']
}

function updateUrl(index, value) {
  const nextUrls = getUrls()
  nextUrls[index] = value
  emit('update:modelValue', nextUrls)
}

function addUrl() {
  emit('update:modelValue', [...getUrls(), ''])
}

function removeUrl(index) {
  const nextUrls = getUrls()

  if (nextUrls.length === 1) {
    emit('update:modelValue', [''])
    return
  }

  nextUrls.splice(index, 1)
  emit('update:modelValue', nextUrls)
}
</script>

<template>
  <div class="image-url-list">
    <div
      v-for="(url, index) in getUrls()"
      :key="index"
      class="image-url-row"
    >
      <el-input
        :model-value="url"
        placeholder="请输入图片 URL"
        @update:model-value="updateUrl(index, $event)"
      />

      <el-button plain @click="removeUrl(index)">删除</el-button>
    </div>

    <el-button type="primary" plain class="add-button" @click="addUrl">
      添加图片 URL
    </el-button>
  </div>
</template>

<style scoped>
.image-url-list {
  display: grid;
  gap: 10px;
}

.image-url-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.add-button {
  justify-self: start;
}

@media (max-width: 640px) {
  .image-url-row {
    grid-template-columns: 1fr;
  }
}
</style>
