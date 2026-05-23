<script setup lang="ts">
import { marked } from 'marked'
import { computed } from 'vue'
import type { EditorMode } from '@/types'

interface Props {
  content: string
  contentMarkdown?: string
  editorMode?: EditorMode
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  editorMode: 'richtext',
  className: '',
})

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
})

const renderContent = computed(() => {
  // 如果是 Markdown 模式且有 Markdown 内容，优先渲染 Markdown
  if (props.editorMode === 'markdown' && props.contentMarkdown) {
    return marked.parse(props.contentMarkdown) as string
  }
  // 否则使用 HTML 内容
  return props.content
})
</script>

<template>
  <div
    :class="[
      'prose prose-lg max-w-none',
      'prose-headings:font-bold prose-headings:text-gray-900 dark:prose-headings:text-gray-100',
      'prose-p:text-gray-700 dark:prose-p:text-gray-300',
      'prose-a:text-blue-600 dark:prose-a:text-blue-400',
      'prose-strong:text-gray-900 dark:prose-strong:text-gray-100',
      'prose-code:bg-gray-100 dark:prose-code:bg-gray-800',
      'prose-pre:bg-gray-900 dark:prose-pre:bg-gray-900',
      'prose-img:rounded-lg',
      'prose-blockquote:border-l-4 prose-blockquote:border-gray-300 dark:prose-blockquote:border-gray-600',
      'prose-table:border-collapse prose-table:w-full',
      'prose-th:border prose-th:px-4 prose-th:py-2 prose-th:bg-gray-100 dark:prose-th:bg-gray-800',
      'prose-td:border prose-td:px-4 prose-td:py-2',
      className
    ]"
    v-html="renderContent"
  />
</template>

<style scoped>
/* 额外的样式优化 */
:deep(.prose pre) {
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
}

:deep(.prose code) {
  font-size: 0.875rem;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
}

:deep(.prose pre code) {
  padding: 0;
  background: transparent;
}

:deep(.prose img) {
  margin: 1.5rem 0;
}

:deep(.prose blockquote) {
  font-style: italic;
  padding-left: 1rem;
  margin: 1.5rem 0;
}

:deep(.prose table) {
  margin: 1.5rem 0;
}
</style>
