<script setup lang="ts">
/**
 * 统一吸顶页面顶部栏
 * 用法：<PageHeader :breadcrumb-items="[{ label: '首页', to: '/' }, { label: '话题' }]" title="话题详情">
 *          <template #actions>...右侧操作按钮</template>
 *       </PageHeader>
 */
import Breadcrumb from '@/components/Breadcrumb.vue';

interface BreadcrumbItem {
  label: string;
  to?: string;
}

interface Props {
  breadcrumbItems?: BreadcrumbItem[];
  /** 标题（可选，显示在面包屑右侧，常用于吸顶时强化当前位置） */
  title?: string;
  /** 是否吸顶，默认 true */
  sticky?: boolean;
}

withDefaults(defineProps<Props>(), {
  sticky: true
});
</script>

<template>
  <div
    :class="[
      'border-b z-30 backdrop-blur-sm',
      sticky ? 'sticky top-0' : ''
    ]"
    style="background-color: var(--theme-surface); border-color: var(--theme-border);"
  >
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between gap-4">
      <Breadcrumb v-if="breadcrumbItems && breadcrumbItems.length > 0" :items="breadcrumbItems" />
      <span v-else></span>
      <div v-if="title || $slots.actions" class="flex items-center gap-2">
        <span v-if="title" class="text-sm font-medium" style="color: var(--theme-text);">{{ title }}</span>
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
