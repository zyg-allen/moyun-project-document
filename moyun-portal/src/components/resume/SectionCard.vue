<script setup lang="ts">
/**
 * 简历模块 - 模块卡片容器
 * 对应 vue_resume_spec.md §5.2 SectionCard
 * 头部：图标(带颜色) + 标题 + 描述 + 状态标签；主体：表单内容
 */
import { computed } from 'vue';
import type { Component } from 'vue';

type IconColor = 'red' | 'blue' | 'green' | 'purple' | 'amber';
type Status = 'complete' | 'partial' | 'empty' | 'none';

const props = defineProps<{
  icon: Component;
  iconColor?: IconColor;
  title: string;
  desc?: string;
  status?: Status;
  sectionId?: string;
}>();

const iconWrapClass = computed(() => {
  const map: Record<IconColor, string> = {
    red: 'bg-red-50 text-red-600',
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-emerald-50 text-emerald-600',
    purple: 'bg-violet-50 text-violet-600',
    amber: 'bg-amber-50 text-amber-600',
  };
  return map[props.iconColor || 'red'];
});

const statusLabel = computed(() => {
  const map: Record<Status, string> = {
    complete: '已填写',
    partial: '待完善',
    empty: '未上传',
    none: '',
  };
  return map[props.status || 'none'];
});

const statusClass = computed(() => {
  const map: Record<Status, string> = {
    complete: 'bg-emerald-50 text-emerald-600',
    partial: 'bg-amber-50 text-amber-600',
    empty: 'bg-gray-100 text-gray-400',
    none: '',
  };
  return map[props.status || 'none'];
});
</script>

<template>
  <section
    :id="sectionId"
    class="re-section-card"
  >
    <header class="re-section-head">
      <div class="re-section-icon" :class="iconWrapClass">
        <component :is="icon" class="w-4 h-4" />
      </div>
      <div class="flex-1 min-w-0">
        <h2 class="re-section-title">{{ title }}</h2>
        <p v-if="desc" class="re-section-desc">{{ desc }}</p>
      </div>
      <span
        v-if="status && status !== 'none'"
        class="re-section-status"
        :class="statusClass"
      >{{ statusLabel }}</span>
    </header>
    <div class="re-section-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.re-section-card {
  background: #fff;
  border: 1px solid var(--theme-border);
  border-radius: 14px;
  margin-bottom: 20px;
  overflow: hidden;
  transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  scroll-margin-top: 96px;
}
.re-section-card:hover {
  border-color: #d1d5db;
}
.re-section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
  background: #fcfcfd;
}
.re-section-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.re-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--theme-text);
  line-height: 1.3;
}
.re-section-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 1px;
}
.re-section-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.re-section-body {
  padding: 20px;
}
</style>
