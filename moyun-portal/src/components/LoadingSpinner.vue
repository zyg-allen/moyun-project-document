<script setup lang="ts">
/**
 * 统一加载状态组件
 * 用法：<LoadingSpinner /> 或 <LoadingSpinner size="lg" text="加载中..." />
 */
interface Props {
  /** 尺寸：sm=24px / md=32px(默认) / lg=40px / xl=48px */
  size?: 'sm' | 'md' | 'lg' | 'xl'
  /** 提示文案，为空则不显示 */
  text?: string
  /** 容器内边距，默认 py-12 */
  padding?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  text: '加载中...',
  padding: 'md'
})

const sizeMap = {
  sm: 'w-6 h-6 border-2',
  md: 'w-8 h-8 border-2',
  lg: 'w-10 h-10 border-2',
  xl: 'w-12 h-12 border-b-2'
}

const paddingMap = {
  sm: 'py-6',
  md: 'py-12',
  lg: 'py-20'
}
</script>

<template>
  <div :class="['flex flex-col items-center justify-center text-center', paddingMap[padding]]">
    <div
      :class="['animate-spin rounded-full', sizeMap[size]]"
      style="border-color: var(--theme-accent); border-top-color: var(--theme-primary);"
      role="status"
      aria-label="加载中"
    ></div>
    <p v-if="text" :class="size === 'sm' ? 'mt-2 text-xs' : 'mt-4 text-sm'" style="color: var(--theme-text-secondary);">
      {{ text }}
    </p>
  </div>
</template>
