<script setup lang="ts">
import { marked } from 'marked'
import { computed } from 'vue'
import type { EditorMode } from '@/types'
import { sanitizeHTML } from '@/utils/security'

interface Props {
  /** HTML 内容（richtext 模式渲染）。markdown 模式下可省略，仅传 contentMarkdown */
  content?: string
  contentMarkdown?: string
  editorMode?: EditorMode
  className?: string
  /** 阅读列宽约束：限制正文最大宽度以提升长文阅读体验 */
  proseWidth?: 'none' | 'normal' | 'wide'
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  editorMode: 'richtext',
  className: '',
  proseWidth: 'none',
})

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
})

const renderContent = computed(() => {
  let html: string
  // 如果是 Markdown 模式且有 Markdown 内容，优先渲染 Markdown
  if (props.editorMode === 'markdown' && props.contentMarkdown) {
    // 对 marked.parse 输出过 sanitize，防止 XSS
    html = sanitizeHTML(marked.parse(props.contentMarkdown) as string)
  } else {
    // 否则使用 HTML 内容，对原始 HTML 也过 sanitize
    html = sanitizeHTML(props.content)
  }
  // 图片懒加载：为所有 <img> 补 loading="lazy"，避免长文一次性请求大量图片
  // sanitize 已放行 loading 属性；未显式声明 loading 的 img 统一标记为 lazy
  if (html.includes('<img')) {
    html = html.replace(/<img(?![^>]*\sloading=)/gi, '<img loading="lazy"')
  }
  return html
})
</script>

<template>
  <div
    :class="[
      'prose prose-lg',
      // 阅读列宽约束：normal=约65ch适合长文阅读；wide=约80ch平衡；none=不限制
      proseWidth === 'normal' ? 'max-w-3xl mx-auto' : (proseWidth === 'wide' ? 'max-w-4xl mx-auto' : 'max-w-none'),
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
  /* 图片加载前预留占位高度，避免加载完成后页面跳动导致分页/滚动位置错乱 */
  min-height: 120px;
  background-color: var(--theme-surface, #f3f4f6);
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
