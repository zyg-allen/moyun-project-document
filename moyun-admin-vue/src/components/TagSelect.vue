<template>
  <el-select
    v-model="innerTags"
    multiple
    filterable
    allow-create
    default-first-option
    :placeholder="placeholder"
    @change="$emit('input', innerTags)"
  >
    <el-option
      v-for="tag in options"
      :key="tag"
      :label="tag"
      :value="tag"
    />
  </el-select>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: [Array, String],
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择或输入标签'
  },
  options: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['input', 'update:modelValue'])

const innerTags = ref(toArray(props.modelValue))

function toArray(val) {
  if (val == null) return []
  if (Array.isArray(val)) return val
  if (typeof val === 'string') {
    return val
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
  }
  return []
}

watch(
  () => props.modelValue,
  (v) => {
    const arr = toArray(v)
    if (JSON.stringify(arr) !== JSON.stringify(innerTags.value)) {
      innerTags.value = arr
    }
  }
)

watch(innerTags, (v) => {
  emit('update:modelValue', v)
  emit('input', v)
})

onMounted(() => {
  innerTags.value = toArray(props.modelValue)
})
</script>

<style scoped>
</style>
