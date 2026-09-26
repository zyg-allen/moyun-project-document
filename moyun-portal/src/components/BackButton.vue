<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'

interface Props {
  /** 无浏览历史（分享/新标签直达）时的层级回退路径 */
  fallback: string
  /** 按钮文字，默认"返回" */
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  label: '返回',
})

const router = useRouter()

/**
 * 智能返回：站内跳转进入时回上一页（保留来源列表的筛选与滚动位置），
 * 分享链接/新标签直达时走明确层级路径，避免返回落空。
 */
function handleBack() {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push(props.fallback)
  }
}
</script>

<template>
  <button
    @click="handleBack"
    class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm flex-shrink-0 transition hover:opacity-90"
    style="color: var(--theme-text-secondary); border: 1px solid var(--theme-border); background-color: var(--theme-surface);"
    :title="label"
  >
    <ArrowLeft class="w-4 h-4" />
    <span class="hidden sm:inline">{{ label }}</span>
  </button>
</template>
